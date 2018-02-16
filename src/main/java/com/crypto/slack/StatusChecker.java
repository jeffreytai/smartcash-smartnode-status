package com.crypto.slack;

import io.github.bonigarcia.wdm.ChromeDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class StatusChecker {

    /**
     * Logging
     */
    private static final Logger logger = LoggerFactory.getLogger(StatusChecker.class);

    /**
     * Url for retrieving smartnode status
     */
    private final String SMARTNODE_URL = "https://smartcash.bitcoiner.me/smartnodes/list/";

    /**
     * Smart node address to check
     */
    private final String SMARTNODE_ADDRESS = "SYNiLoKVFZAVA1kF3xoHa5UWzTy4tRNYqt";

    /**
     * Timeout in milliseconds
     */
    private final Integer WAIT_IN_MS = 15000;

    /**
     * Number of times to retry connecting to URL
     */
    private final Integer RETRY_COUNT = 3;

    public StatusChecker() {}

    public void checkStatus() {
        WebDriver driver = null;

        for (int retryCount=0; retryCount < this.RETRY_COUNT; ++retryCount) {
            try {
                logger.info("Attempt {} to connect", retryCount);

                // Setup Chrome Driver instance
                logger.info("Setting up Selenium chrome instance");
                ChromeDriverManager.getInstance().setup();
                driver = new ChromeDriver();

                // Retrieve HTML document from page, allow all nodes to load
                logger.info("Parsing HTML document");
                driver.get(this.SMARTNODE_URL);
                Thread.sleep(this.WAIT_IN_MS);;
                Document doc = Jsoup.parse(driver.getPageSource());

                // Find smart node that matches specified address
                Elements smartNodes = doc.select("tbody tr");
                Optional<Element> personalSmartNode = smartNodes.stream().filter(sn -> sn.text().contains(this.SMARTNODE_ADDRESS)).findFirst();
                if (personalSmartNode.isPresent()) {
                    Element smartNode = personalSmartNode.get();

                    Elements smartNodeDetails = smartNode.getElementsByClass("bookmark").first().siblingElements();

                    String status = smartNodeDetails.get(1).text();
                    if (!status.equals("ENABLED")) {
                        sendSlackAlert(status);
                    }
                }
            } catch (InterruptedException ex) {}
            finally {
                // Close created chrome instances
                if (driver != null) {
                    logger.info("Closing chrome instance");;
                    driver.close();
                }
            }
        }
    }

    /**
     * Send notification to slack channel about smartnode status
     * @param status
     */
    private void sendSlackAlert(String status) {
        SlackWebhook slack = new SlackWebhook("smartnode-status-alert");

        String message = String.format("SmartNode status: %s", status);
        slack.sendMessage(message);

        slack.shutdown();
    }
}
