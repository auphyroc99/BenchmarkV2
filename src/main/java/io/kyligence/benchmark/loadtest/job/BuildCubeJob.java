package io.kyligence.benchmark.loadtest.job;

import io.kyligence.benchmark.loadtest.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by xiefan on 17-1-4.
 */
public class BuildCubeJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(BuildCubeJob.class);

    private TestCase testCase;

    private String cubeName;

    private long startTime;

    private long endTime;

    private long checkInterval;

    private static long BUILD_TIME_OUT = 3 * 3600 * 1000; // 3h

    public BuildCubeJob(TestCase testCase) {
        this.testCase = testCase;
        this.cubeName = testCase.getCubeName();
        this.startTime = testCase.getBuildStartTime();
        this.endTime = testCase.getBuildEndTime();
        this.checkInterval = testCase.getSheckCubeStatusInterval();
    }

    @Override
    public boolean run() throws Exception {
        logger.info("Start to run BuildCubeJob");
        final RestClient restClient = new RestClient(testCase);
        String originalCubeStatus = getCubeStatus(restClient.getCube(cubeName));
        logger.info("cube name : {}. original status : {}", cubeName, originalCubeStatus);
        if (originalCubeStatus.equals("READY"))
            restClient.disableCube(cubeName);
        restClient.purgeCube(cubeName);
        restClient.buildCube(cubeName, startTime, endTime, "BUILD");
        HashMap<?, ?> cube = restClient.getCube(cubeName);
        long startTime = System.currentTimeMillis();
        while (!getCubeStatus(cube).equals("READY")) {
            logger.info("Waiting cube {} ready.  current status : {}", cubeName, cube);
            Thread.currentThread();
            Thread.sleep(checkInterval);
            cube = restClient.getCube(cubeName);
            if (getCubeStatus(cube).equals("ERROR")) {
                throw new RuntimeException("BUILD CUBE ERROR. Exit the stress test program");
            }
            if (System.currentTimeMillis() - startTime > BUILD_TIME_OUT) {
                throw new RuntimeException("Build Cube timeout. Please check whether there are something wrong");
            }
        }
        logger.info("BuildCubeJob end");
        return true;
    }

    @Override
    public void dump() {
        logger.info("-----------------BuildCubeJob dump--------------------");
    }

    private String getCubeStatus(HashMap<?, ?> map) {
        return (String) ((HashMap<?, ?>) map.get("data")).get("status");
    }

}
