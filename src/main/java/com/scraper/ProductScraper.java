package com.scraper;

import java.time.Duration;
import java.util.List;

import org.jsoup.Jsoup;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.jsoup.select.Elements;

import com.crawler.WebCrawler.CrawlResult;
import com.threading.WorkQueue;

public class ProductScraper {
	public static final int MAX_ELEMENT_TIMEOUT_DEFAULT = 5;
	
	public ProductScraper() {
		System.setProperty("webdriver.chrome.driver", "C:/Users/domin/chromedriver.exe");
		System.setProperty("webdriver.chrome.whitelistedIps", "");
		System.setProperty("webdriver.chrome.silentOutput", "true");
	}
	
	private class Task implements Runnable {
		private final CrawlResult crawlResult;
		private final List<ConfigField> config;
		private final List<ScrapeResult> scrapeResults;
		
		private Task(
			CrawlResult crawlResult, 
			List<ConfigField> config, 
			List<ScrapeResult> scrapeResults
		) {
			this.crawlResult = crawlResult;
			this.config = config;
			this.scrapeResults = scrapeResults;
		}

		@Override
		public void run() {
			try {
				scrape(this.crawlResult.getUrl(), this.config, this.scrapeResults, true);
			} catch (Exception e) {
				System.out.println("Exception occurred in ProductScraper task: " + e.getMessage());
				e.printStackTrace();
			}
		}
		
	}
	
	public void scrape(List<CrawlResult> crawlResults, List<ConfigField> config, List<ScrapeResult> scrapeResults) {
		for (CrawlResult crawlResult : crawlResults) {
			try {
				this.scrape(crawlResult.getUrl(), config, scrapeResults, false);
			} catch (Exception e) {
				System.out.println("Exception occurred in ProductScraper: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public void scrape(List<CrawlResult> crawlResults, List<ConfigField> config, List<ScrapeResult> scrapeResults, WorkQueue queue) throws Exception {
		for (CrawlResult crawlResult : crawlResults) {
			queue.execute(new Task(crawlResult, config, scrapeResults));
		}
	}
	
	private void scrape(String url, List<ConfigField> config, List<ScrapeResult> scrapeResults, boolean isMultiThreaded) throws Exception {
		String html = Jsoup.connect(url).get().html();
		org.jsoup.nodes.Document doc = Jsoup.parse(html);
		
        ScrapeResult scrapeResult = new ScrapeResult(url);
        for (ConfigField field : config) {
        	org.jsoup.nodes.Element element = null;
        	
        	String selectorType = field.getSelectorType();
        	String selectorValue = field.getSelectorValue();
        	String name = field.getName();
        	String value = field.getFallbackValue();
        	String dataType = field.getDataType();
        	
        	if (selectorType.equals("ID")) {
        		element = doc.getElementById(selectorValue);
        	} else if (selectorType.equals("XPATH")) {
        		Elements elements = doc.selectXpath(selectorValue);
        		if (elements.size() > 0) {
        			element = elements.get(0);
        		}
        	}
        	
        	if (element != null) {
        		if (dataType.equals("text")) {
        			value = element.text();
        		} else if (dataType.equals("image")) {
        			value = element.attr("src");
        		}
        	}
        	
        	scrapeResult.addFeature(name, value);
        }

        
        if (isMultiThreaded) {
	        synchronized (scrapeResults) {
	        	scrapeResults.add(scrapeResult);
	        }
        } else {
        	scrapeResults.add(scrapeResult);
        }
	}
	
	/**
	 * @deprecated
	 * @param url
	 * @param config
	 * @param scrapeResults
	 * @param isMultiThreaded
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private void scrapeHelper(String url, List<ConfigField> config, List<ScrapeResult> scrapeResults, boolean isMultiThreaded) throws Exception {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--remote-allow-origins=*");
		options.addArguments("--window-size=1920,1080");
		System.setProperty("webdriver.chrome.timeout", "60000");
		options.setHeadless(true);
		String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.50 Safari/537.36";
		options.addArguments("user-agent=" + userAgent);

		WebDriver driver = new ChromeDriver(options);
		
		driver.get(url);
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofSeconds(1))
                .ignoring(NoSuchElementException.class);
        
        ScrapeResult scrapeResult = new ScrapeResult(url);
        for (ConfigField field : config) {
        	WebElement element = null;
        	
        	String selectorType = field.getSelectorType();
        	String selectorValue = field.getSelectorValue();
        	String name = field.getName();
        	String value = field.getFallbackValue();
        	String dataType = field.getDataType();
        	
        	if (selectorType.equals("ID")) {
        		element = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(selectorValue)));
        	} else if (selectorType.equals("XPATH")) {
        		element = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(selectorValue)));
        	}
        	
        	if (element != null) {
        		if (dataType.equals("text")) {
        			value = element.getText();
        		} else if (dataType.equals("image")) {
        			value = element.getAttribute("src");
        		}
        	}
        	
        	scrapeResult.addFeature(name, value);
        }
        
        driver.quit();
        
        if (isMultiThreaded) {
	        synchronized (scrapeResults) {
	        	scrapeResults.add(scrapeResult);
	        }
        } else {
        	scrapeResults.add(scrapeResult);
        }
	}
}
