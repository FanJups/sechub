// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.report;

import static com.daimler.sechub.sharedkernel.UUIDTraceLogID.*;
import static com.daimler.sechub.sharedkernel.util.Assert.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daimler.sechub.commons.model.TrafficLight;
import com.daimler.sechub.domain.scan.ReportTransformationResult;
import com.daimler.sechub.domain.scan.SecHubReportProductTransformerService;
import com.daimler.sechub.domain.scan.product.ReportProductExecutionService;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionContext;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionException;

@Service
public class CreateScanReportService {

	private static final Logger LOG = LoggerFactory.getLogger(CreateScanReportService.class);

	@Autowired
	SecHubReportProductTransformerService reportTransformerService;

	@Autowired
	ReportProductExecutionService reportProductExecutionService;

	@Autowired
	ScanReportTrafficLightCalculator trafficLightCalculator;

	@Autowired
	ScanReportRepository reportRepository;
	
	@Autowired
	ScanReportTransactionService scanReportTransactionService;

	/**
	 * Creates a report based on product results. There is no security check because its only called internally from system.
	 *
	 * @param context
	 * @return report, never <code>null</code>
	 * @throws ScanReportException
	 */
	public ScanReport createReport(SecHubExecutionContext context) throws ScanReportException {
		notNull(context, "Context may not be null!");

		UUID sechubJobUUID = context.getSechubJobUUID();
		if (sechubJobUUID == null) {
			throw new ScanReportException("Cannot create a report for Job UUID:null");
		}
		LOG.info("Creating report for {}, will delete former reports if existing", traceLogID(sechubJobUUID));
		
		/* we allow only one report for one job */
		scanReportTransactionService.deleteAllReportsForSecHubJobUUIDinOwnTransaction(sechubJobUUID);
		
		/* create report - project id in configuration was set on job creation time and is always correct/valid and
		 * will differ between api parameter and config..!*/
		ScanReport report = new ScanReport(sechubJobUUID,context.getConfiguration().getProjectId());
		report.setStarted(LocalDateTime.now());

		/* execute report products */
		try {
			reportProductExecutionService.executeProductsAndStoreResults(context);
		} catch (SecHubExecutionException e) {
			throw new ScanReportException("Report product execution failed", e);
		}
		
		/* transform */
		ReportTransformationResult reportTransformerResult;
		try {
			reportTransformerResult = reportTransformerService.createResult(context);
			report.setResult(reportTransformerResult.getResult().toJSON());
			
		} catch (Exception e) {
			throw new ScanReportException("Was not able to build sechub result", e);
		}

		/* create and set the traffic light */
		TrafficLight trafficLight = trafficLightCalculator.calculateTrafficLight(reportTransformerResult);
		report.setTrafficLight(trafficLight);

		/* update time stamp*/
		report.setEnded(LocalDateTime.now());

		/* persist */
		return reportRepository.save(report);
	}

}
