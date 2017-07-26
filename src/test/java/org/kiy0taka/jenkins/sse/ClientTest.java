package org.kiy0taka.jenkins.sse;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.TopLevelItem;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * @author Kiyotaka Oku
 */
public class ClientTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private List<String> called = new ArrayList<>();

//    @Test
    public void subscribeJobEvents() throws Exception {
        newClient().subscribe("job", this::handleEvent);

        FreeStyleProject job = j.createFreeStyleProject("test_job");
        buildAndWait(job);

        assertEquals(asList(
            "job,test_job,job_run_queue_buildable",
            "job,test_job,job_run_queue_left",
            "job,test_job,job_run_started",
            "job,test_job,job_run_ended"
        ), called);
    }

    @Test
    @LocalData
    public void subscribePipelineEvents() throws Exception {
        newClient().subscribe("pipeline", event -> {
            Map data = event.readData(Map.class);
            called.add(String.format("%s,%s,%s,%s,%s",
                    data.get("jenkins_channel"),
                    data.get("pipeline_job_name"),
                    data.get("jenkins_event"),
                    data.get("pipeline_step_stage_name"),
                    data.get("pipeline_step_name")));
        });

        WorkflowJob pipeline = (WorkflowJob) j.jenkins.getItem("test_pipeline");
        buildAndWait(pipeline);

        assertEquals(asList(
                "job,test_job,job_run_queue_buildable",
                "job,test_job,job_run_queue_left",
                "job,test_job,job_run_started",
                "job,test_job,job_run_ended"
        ), called);
    }

    private Client newClient() throws IOException {
        Client client = new Client(j.getURL().toExternalForm());
        client.connect(ClientTest.class.getCanonicalName());
        return client;
    }

    private void handleEvent(InboundEvent event) {
        Map data = event.readData(Map.class);
        System.out.println(data);
        called.add(String.format("%s,%s,%s", data.get("jenkins_channel"), data.get("job_name"), data.get("jenkins_event")));
    }

    private void buildAndWait(Project project) throws ExecutionException, InterruptedException {
        Build build = (Build) project.scheduleBuild2(5).get();
        while (build.getResult().isCompleteBuild() == false) {
            Thread.sleep(100);
        }
        Thread.sleep(100);
    }

    private void buildAndWait(WorkflowJob project) throws ExecutionException, InterruptedException {
        WorkflowRun build = (WorkflowRun) project.scheduleBuild2(5).get();
        while (build.getResult().isCompleteBuild() == false) {
            Thread.sleep(100);
        }
        Thread.sleep(100);
    }

}
