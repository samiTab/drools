package org.drools.analytics;

import java.util.Collection;

import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.analytics.dao.AnalyticsResult;
import org.drools.analytics.dao.AnalyticsResultFactory;
import org.drools.analytics.report.ReportModeller;
import org.drools.analytics.report.html.ComponentsReportModeller;
import org.drools.lang.descr.PackageDescr;
import org.drools.rule.Package;

/**
 * This is the main user class for analysis.
 * This will use rules to validate rules, caching the "knowledge base" of analysis rules.
 *
 * @author Toni Rikkola
 */
public class Analyzer {

	static RuleBase analysisKnowledgeBase;


	private AnalyticsResult result = AnalyticsResultFactory
			.createAnalyticsResult();

	public void addPackageDescr(PackageDescr descr) {
		try {

			PackageDescrFlattener ruleFlattener = new PackageDescrFlattener();

			ruleFlattener.addPackageDescrToData(descr, result
					.getAnalyticsData());

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}


	/**
	 * As the analyzer uses rules itself, this will reload the knowledge base.
	 * @throws Exception
	 */
	public synchronized void reloadAnalysisKnowledgeBase() throws Exception {
		analysisKnowledgeBase = createRuleBase();
	}

	/**
	 * This will run the analysis.
	 */
	public void fireAnalysis() {
		try {

			if (this.analysisKnowledgeBase == null) {
				synchronized (this.getClass()) {
					analysisKnowledgeBase = createRuleBase();
				}
			}

			WorkingMemory workingMemory = analysisKnowledgeBase.newStatefulSession();

			Collection<? extends Object> c = result.getAnalyticsData().getAll();

			for (Object o : c) {
				workingMemory.insert(o);
			}

			// Object that returns the results.
			workingMemory.setGlobal("result", result);
			workingMemory.fireAllRules();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Returns the analysis results as plain text.
	 *
	 * @return Analysis results as plain text.
	 */
	public String getResultAsPlainText() {
		return ReportModeller.writePlainText(result);
	}

	/**
	 * Returns the analysis results as XML.
	 *
	 * @return Analysis results as XML
	 */
	public String getResultAsXML() {
		return ReportModeller.writeXML(result);
	}

	/**
	 * Returns the analysis results as HTML.
	 *
	 * @return Analysis results as HTML
	 */
	public void writeComponentsHTML(String path) {
		ComponentsReportModeller.writeHTML(path, result);
	}

	/**
	 * Returns the analysis results as <code>AnalysisResult</code> object.
	 *
	 * @return Analysis result
	 */
	public AnalyticsResult getResult() {
		return result;
	}

	private static RuleBase createRuleBase() throws Exception {

		RuleBase ruleBase = RuleBaseFactory.newRuleBase();

		Collection<Package> packages = RuleLoader.loadPackages();
		for (Package pkg : packages) {
			try {
				ruleBase.addPackage(pkg);
			} catch (Exception e) {
				throw new Exception("Adding package " + pkg.getName()
						+ " caused an error.", e);
			}
		}

		return ruleBase;
	}
}
