package io.kestros.cms.foundation.design.htltemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

/**
 * Structured HTL template tags, so that they can be compiled and managed by VendorLibraries and
 * UiFrameworks.
 */
public class HtlTemplate {

  public static final String ATTRIBUTE_DATA_SLY_TEMPLATE = "data-sly-template.";

  private final Node node;
  private final String sourcePath;

  private static final String CALL_VARIABLES_INDENT = "     ";

  HtlTemplate(final Node node, final String sourcePath) {
    this.node = node;
    this.sourcePath = sourcePath;
  }

  /**
   * HTL Template's origination file.
   *
   * @return HTL Template's origination file.
   */
  public String getSourcePath() {
    return this.sourcePath;
  }

  /**
   * HTL Template name.
   *
   * @return HTL Template name.
   */
  public String getName() {
    final Attributes attributes = this.node.attributes();

    for (final Attribute attribute : attributes) {
      final String key = attribute.getKey();
      if (key.startsWith(ATTRIBUTE_DATA_SLY_TEMPLATE)) {
        return key.split(ATTRIBUTE_DATA_SLY_TEMPLATE)[1];
      }
    }
    return null;
  }

  /**
   * Title of the current template, or template name if blank.
   *
   * @return Title of the current template.
   */
  public String getTitle() {
    final Attributes attributes = this.node.attributes();
    final String title = attributes.get("data-title");
    if (StringUtils.isNotBlank(title)) {
      return title;
    }
    return getName();
  }

  /**
   * Description of the current template.
   *
   * @return Description of the current template.
   */
  public String getDescription() {
    final Attributes attributes = this.node.attributes();
    final String title = attributes.get("data-description");
    if (StringUtils.isNotBlank(title)) {
      return title;
    }
    return StringUtils.EMPTY;
  }

  /**
   * Full HTML output of the HTL Template.
   *
   * @return Full HTML output of the HTL Template.
   */
  public String getOutput() {
    return this.node.toString();
  }

  /**
   * HTML Output of the current template after it is called from the implementing script.
   *
   * @return HTML Output of the current template after it is called from the implementing script.
   */
  public String getHtmlOutput() {
    final StringBuilder htmlOutputStringBuilder = new StringBuilder();
    for (final Node child : this.node.childNodes()) {
      String childHtml = child.toString();
      childHtml = childHtml.replaceAll("data-", "\ndata-");
      htmlOutputStringBuilder.append(childHtml);
    }

    String htmlOutput = htmlOutputStringBuilder.toString();
    if (htmlOutput.startsWith("\n")) {
      htmlOutput = htmlOutput.replaceFirst("\n", "");
    }
    final Document htmlOutputDocument = Jsoup.parseBodyFragment(htmlOutput);
    htmlOutputDocument.outputSettings().outline(true);
    htmlOutputDocument.outputSettings().prettyPrint(false);

    return htmlOutputDocument.body().html();
  }

  /**
   * List of variables required to implement the current Template.
   *
   * @return List of variables required to implement the current Template.
   */
  public List<String> getVariables() {
    final Attributes attributes = this.node.attributes();

    String value = "";
    for (final Attribute attribute : attributes) {
      final String key = attribute.getKey();
      if (key.startsWith(ATTRIBUTE_DATA_SLY_TEMPLATE)) {
        value = attribute.getValue();
      }
    }

    value = value.replaceAll("\\$", "");
    value = value.replaceAll("\\{", "");
    value = value.replaceAll("\\}", "");
    value = value.replaceAll("@", "");
    value = value.replaceAll(" ", "");

    return Arrays.asList(value.split(","));
  }

  /**
   * Sample usage script.
   *
   * @return Sample usage script.
   */
  public String getSampleUsage() {
    final StringBuilder sampleUsage = new StringBuilder();
    sampleUsage.append("<sly data-sly-call=\"${templates.");
    sampleUsage.append(getName());
    sampleUsage.append(" @");
    String prefix = "\n" + CALL_VARIABLES_INDENT;
    for (final String variable : getVariables()) {
      sampleUsage.append(prefix);
      prefix = ",\n" + CALL_VARIABLES_INDENT;
      sampleUsage.append(variable);
      sampleUsage.append("=");
      final String value = "my" + variable.substring(0, 1).toUpperCase(Locale.ENGLISH)
                           + variable.substring(1);
      sampleUsage.append(value);
    }
    sampleUsage.append("}\" />");
    return sampleUsage.toString();
  }
}