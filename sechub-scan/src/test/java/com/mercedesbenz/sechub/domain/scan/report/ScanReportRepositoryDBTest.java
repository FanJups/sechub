// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.domain.scan.report;

import static com.mercedesbenz.sechub.test.FlakyOlderThanTestWorkaround.*;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = { ScanReportRepository.class, ScanReportRepositoryDBTest.SimpleTestConfiguration.class })
public class ScanReportRepositoryDBTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ScanReportRepository repositoryToTest;

    @Before
    public void before() {
    }

    @Test
    public void test_data_4_jobs_delete_1_day_still_has_2() throws Exception {
        /* prepare */
        DeleteScanReportTestData testData = new DeleteScanReportTestData();
        testData.createAndCheckAvailable();
        
        LocalDateTime olderThan = olderThanForDelete(testData.before_1_day);

        /* execute */
        int deleted = repositoryToTest.deleteReportsOlderThan(olderThan);
        repositoryToTest.flush();

        /* test */
        assertDeleted(2, deleted, testData, olderThan);
        List<ScanReport> allJobsNow = repositoryToTest.findAll();
        assertTrue(allJobsNow.contains(testData.job3_1_day_before_created));
        assertTrue(allJobsNow.contains(testData.job4_now_created));
        assertEquals(2, allJobsNow.size());
    }

    @Test
    public void test_data_4_jobs_delete_1_day_before_plus1_second_still_has_1() throws Exception {
        /* prepare */
        DeleteScanReportTestData testData = new DeleteScanReportTestData();
        testData.createAndCheckAvailable();

        /* execute */
        LocalDateTime olderThan = testData.before_1_day.plusSeconds(1);
        int deleted = repositoryToTest.deleteReportsOlderThan(olderThan);
        repositoryToTest.flush();

        /* test */
        assertDeleted(3, deleted, testData, olderThan);
        List<ScanReport> allJobsNow = repositoryToTest.findAll();
        assertTrue(allJobsNow.contains(testData.job4_now_created));
        assertEquals(1, allJobsNow.size());
    }

    @Test
    public void test_data_4_jobs_delete_1_day_before_plus1_second_deletes_3() throws Exception {
        /* prepare */
        DeleteScanReportTestData testData = new DeleteScanReportTestData();
        testData.createAndCheckAvailable();
        
        LocalDateTime olderThan = testData.before_1_day.plusSeconds(1);

        /* execute */
        int deleted = repositoryToTest.deleteReportsOlderThan(olderThan);
        repositoryToTest.flush();

        /* test */
        assertDeleted(3, deleted, testData, olderThan);
    }

    @Test
    public void test_data_4_jobs_oldest_90_days_delete_90_days_still_has_4() throws Exception {
        /* prepare */
        DeleteScanReportTestData testData = new DeleteScanReportTestData();
        testData.createAndCheckAvailable();
        
        LocalDateTime olderThan = olderThanForDelete(testData.before_90_days);
        
        /* execute */
        int deleted = repositoryToTest.deleteReportsOlderThan(olderThan);
        repositoryToTest.flush();

        /* test */
        assertDeleted(0, deleted, testData, olderThan);
        List<ScanReport> allJobsNow = repositoryToTest.findAll();
        assertTrue(allJobsNow.contains(testData.job1_90_days_before_created));
        assertTrue(allJobsNow.contains(testData.job2_2_days_before_created));
        assertTrue(allJobsNow.contains(testData.job3_1_day_before_created));
        assertTrue(allJobsNow.contains(testData.job4_now_created));
        assertEquals(4, allJobsNow.size());
    }

    @Test
    public void test_data_4_jobs_oldest_90_days_delete_90_days_deletes_0() throws Exception {
        /* prepare */
        DeleteScanReportTestData testData = new DeleteScanReportTestData();
        testData.createAndCheckAvailable();
        
        LocalDateTime olderThan = olderThanForDelete(testData.before_90_days);
        
        /* execute */
        int deleted = repositoryToTest.deleteReportsOlderThan(olderThan);
        repositoryToTest.flush();

        /* test */
        assertDeleted(0, deleted, testData, olderThan);
    }

    @Test
    public void test_data_4_jobs_oldest_90_days_delete_89_days() throws Exception {
        /* prepare */
        DeleteScanReportTestData testData = new DeleteScanReportTestData();
        testData.createAndCheckAvailable();

        LocalDateTime olderThan = testData.before_89_days;
        
        /* execute */
        int deleted = repositoryToTest.deleteReportsOlderThan(olderThan);
        repositoryToTest.flush();

        /* test */
        assertDeleted(1, deleted, testData, olderThan);
        List<ScanReport> allJobsNow = repositoryToTest.findAll();
        assertTrue(allJobsNow.contains(testData.job2_2_days_before_created));
        assertTrue(allJobsNow.contains(testData.job3_1_day_before_created));
        assertTrue(allJobsNow.contains(testData.job4_now_created));
        assertEquals(3, allJobsNow.size());
    }

    @Test
    public void test_data_4_jobs_oldest_90_days_delete_1_day() throws Exception {
        /* prepare */
        DeleteScanReportTestData testData = new DeleteScanReportTestData();
        testData.createAndCheckAvailable();

        LocalDateTime olderThan = testData.before_89_days;
        
        /* execute */
        int deleted = repositoryToTest.deleteReportsOlderThan(olderThan);
        repositoryToTest.flush();

        /* test */
        assertDeleted(1, deleted, testData, olderThan);
        List<ScanReport> allJobsNow = repositoryToTest.findAll();
        assertTrue(allJobsNow.contains(testData.job2_2_days_before_created));
        assertTrue(allJobsNow.contains(testData.job3_1_day_before_created));
        assertTrue(allJobsNow.contains(testData.job4_now_created));
        assertEquals(3, allJobsNow.size());
    }

    @Test
    public void given_3_stored_scan_reports_2_for_project1_1_for_project2_a_delete_all_for_project1_does_only_delete_project1_parts() throws Exception {
        /* prepare */
        UUID job1_project1 = UUID.randomUUID();
        UUID job2_project2 = UUID.randomUUID();
        UUID job3_project1 = UUID.randomUUID();

        ScanReport result1 = new ScanReport(job1_project1, "project1");
        result1.setResult("r1");
        ScanReport result2 = new ScanReport(job2_project2, "project2");
        result2.setResult("r2");
        ScanReport result3 = new ScanReport(job3_project1, "project1");
        result3.setResult("r3");

        repositoryToTest.save(result1);
        repositoryToTest.save(result2);
        repositoryToTest.save(result3);

        /* check preconditions */
        assertEquals(3, repositoryToTest.count());
        assertNotNull(repositoryToTest.findById(job2_project2));

        /* execute */
        repositoryToTest.deleteAllReportsForProject("project1");

        /* test */
        assertEquals(1, repositoryToTest.count());
        assertNotNull(repositoryToTest.findById(job2_project2));
    }

    private void assertDeleted(int expected, int deleted, DeleteScanReportTestData testData, LocalDateTime olderThan) {
        if (deleted == expected) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        List<ScanReport> all = repositoryToTest.findAll();
        sb.append("Delete call did return ").append(deleted).append(" uploadMaximumBytes was ").append(expected).append("\n");
        sb.append("The remaining entries are:\n");
        for (ScanReport info : all) {
            sb.append(resolveName(info.started, testData)).append("- since       : ").append(info.started).append("\n");
        }
        sb.append("\n-----------------------------------------------------");
        sb.append("\nolderThan was: ").append(olderThan).append(" - means :").append((resolveName(olderThan, testData)));
        sb.append("\n-----------------------------------------------------\n");
        sb.append(describe(testData.job1_90_days_before_created, testData));
        sb.append(describe(testData.job2_2_days_before_created, testData));
        sb.append(describe(testData.job3_1_day_before_created, testData));
        sb.append(describe(testData.job4_now_created, testData));
    
        fail(sb.toString());
    }

    private String describe(ScanReport info, DeleteScanReportTestData data) {
        return resolveName(info.started, data) + " - created: " + info.started + "\n";
    }

    private String resolveName(LocalDateTime time, DeleteScanReportTestData data) {
        if (data.job1_90_days_before_created.started.equals(time)) {
            return "job1_90_days_before_created";
        }
        if (data.job2_2_days_before_created.started.equals(time)) {
            return "job2_2_days_before_created";
        }
        if (data.job3_1_day_before_created.started.equals(time)) {
            return "job3_1_day_before_created";
        }
        if (data.job4_now_created.started.equals(time)) {
            return "job4_now_created";
        }
        return null;
    }
    
    private class DeleteScanReportTestData {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime before_89_days = now.minusDays(89);
        LocalDateTime before_90_days = now.minusDays(90);
        LocalDateTime before_3_days = now.minusDays(3);
        LocalDateTime before_1_day = now.minusDays(1);

        ScanReport job1_90_days_before_created;
        ScanReport job2_2_days_before_created;
        ScanReport job3_1_day_before_created;
        ScanReport job4_now_created;

        private void createAndCheckAvailable() {
            job1_90_days_before_created = create(before_90_days);
            job2_2_days_before_created = create(before_3_days);
            job3_1_day_before_created = create(before_1_day);
            job4_now_created = create(now);

            // check preconditions
            repositoryToTest.flush();
            assertEquals(4, repositoryToTest.count());
            List<ScanReport> allJobsNow = repositoryToTest.findAll();
            assertTrue(allJobsNow.contains(job1_90_days_before_created));
            assertTrue(allJobsNow.contains(job2_2_days_before_created));
            assertTrue(allJobsNow.contains(job3_1_day_before_created));
            assertTrue(allJobsNow.contains(job4_now_created));
        }

        private ScanReport create(LocalDateTime since) {
            ScanReport scanReport = new ScanReport();
            scanReport.started = since;
            scanReport.projectId = "project1";
            scanReport.secHubJobUUID = UUID.randomUUID();
            entityManager.persist(scanReport);
            entityManager.flush();
            return scanReport;
        }
    }

    @TestConfiguration
    @EnableAutoConfiguration
    public static class SimpleTestConfiguration {

    }

}
