package edu.hm.hafner.analysis.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import j2html.tags.ContainerTag;

import static j2html.TagCreator.*;

/**
 * <p>
 * Parser for reports of pnpm audit scans.
 * </p>
 * <p>
 * <strong>Usage: </strong>pnpm audit --json &gt; pnpm-audit.json
 * </p>
 *
 * @author Fabian Kaupp - kauppfbi@gmail.com
 */
public class PnpmAuditParser extends JsonIssueParser {

    private static final String VALUE_NOT_SET = "-";
    private static final String UNCATEGORIZED = "Uncategorized";

    private static final String PNPM_VULNERABILITY_SEVERITY_INFO = "info";
    private static final String PNPM_VULNERABILITY_SEVERITY_LOW = "low";
    private static final String PNPM_VULNERABILITY_SEVERITY_MODERATE = "moderate";
    private static final String PNPM_VULNERABILITY_SEVERITY_HIGH = "high";
    private static final String PNPM_VULNERABILITY_SEVERITY_CRITICAL = "critical";

    private static final long serialVersionUID = 4140706319863200922L;

    @Override
    protected void parseJsonObject(final Report report, final JSONObject jsonReport, final IssueBuilder issueBuilder) {
        JSONObject results = jsonReport.optJSONObject("advisories");
        if (results != null) {
            parseResults(report, results, issueBuilder);
        }
    }

    private void parseResults(final Report report, final JSONObject jsonReport, final IssueBuilder issueBuilder) {
        for (String key : jsonReport.keySet()) {
            JSONObject vulnerability = (JSONObject) jsonReport.get(key);

            if (!vulnerability.isEmpty()) {
                report.add(convertToIssue(vulnerability, issueBuilder));
            }
        }
    }

    private Issue convertToIssue(final JSONObject vulnerability, final IssueBuilder issueBuilder) {
        return issueBuilder.setModuleName(vulnerability.optString("module_name", VALUE_NOT_SET))
                .setCategory(formatCategory(vulnerability))
                .setSeverity(mapSeverity(vulnerability.optString("severity", "UNKNOWN")))
                .setType(mapType(vulnerability))
                .setMessage(vulnerability.optString("overview", "UNKNOWN"))
                .setDescription(formatDescription(vulnerability))
                .buildAndClean();
    }

    private String mapType(final JSONObject vulnerability) {
        final JSONArray cves = vulnerability.optJSONArray("cves");

        return cves != null && !cves.isNull(0) ? (String) cves.opt(0) : UNCATEGORIZED;
    }

    private String formatCategory(final JSONObject vulnerability) {
        String moduleName = vulnerability.optString("module_name");
        String title = vulnerability.optString("title");

        if (moduleName != null && title != null) {
            return title.replace(moduleName, "").replace(" in ", "");
        }

        return VALUE_NOT_SET;
    }

    @SuppressFBWarnings("IMPROPER_UNICODE")
    private Severity mapSeverity(final String string) {
        if (PNPM_VULNERABILITY_SEVERITY_INFO.equalsIgnoreCase(string)) {
            return Severity.WARNING_LOW;
        }
        else if (PNPM_VULNERABILITY_SEVERITY_LOW.equalsIgnoreCase(string)) {
            return Severity.WARNING_LOW;
        }
        else if (PNPM_VULNERABILITY_SEVERITY_MODERATE.equalsIgnoreCase(string)) {
            return Severity.WARNING_NORMAL;
        }
        else if (PNPM_VULNERABILITY_SEVERITY_HIGH.equalsIgnoreCase(string)) {
            return Severity.WARNING_HIGH;
        }
        else if (PNPM_VULNERABILITY_SEVERITY_CRITICAL.equalsIgnoreCase(string)) {
            return Severity.ERROR;
        }
        else {
            return Severity.WARNING_NORMAL;
        }
    }

    private String formatDescription(final JSONObject vulnerability) {
        final List<ContainerTag> vulnerabilityTags = new ArrayList<>();

        getValueAsContainerTag(vulnerability, "module_name", "Module").ifPresent(vulnerabilityTags::add);

        final JSONArray findings = vulnerability.optJSONArray("findings");
        if (findings != null && !findings.isEmpty()) {
            final JSONObject firstFinding = (JSONObject) findings.opt(0);
            final String installedVersion = firstFinding.optString("version");

            getValueAsContainerTag(installedVersion, "Installed Version").ifPresent(vulnerabilityTags::add);
        }

        getValueAsContainerTag(vulnerability, "vulnerable_versions", "Vulnerable Versions").ifPresent(
                vulnerabilityTags::add);
        getValueAsContainerTag(vulnerability, "patched_versions", "Patched Versions").ifPresent(vulnerabilityTags::add);
        getValueAsContainerTag(vulnerability, "severity", "Severity").ifPresent(vulnerabilityTags::add);
        getValueAsContainerTag(vulnerability, "overview").ifPresent(vulnerabilityTags::add);
        getValueAsContainerTag(vulnerability, "references", "References").ifPresent(vulnerabilityTags::add);

        return join(p(join(vulnerabilityTags.toArray()))).render();
    }

    private Optional<ContainerTag> getValueAsContainerTag(final JSONObject vulnerability, final String tagOfValue) {
        final String value = vulnerability.optString(tagOfValue);

        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(p(text(value)));
    }

    private Optional<ContainerTag> getValueAsContainerTag(final String value, final String label) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(div(b(label + ": "), text(value)));
    }

    private Optional<ContainerTag> getValueAsContainerTag(final JSONObject vulnerability, final String tagOfValue,
            final String label) {
        final String value = vulnerability.optString(tagOfValue);

        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(div(b(label + ": "), text(value)));
    }
}