package demo.wrappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;
import io.opentelemetry.semconv.SemanticAttributes.SystemMemoryStateValues;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;

public class Wrappers {
    /*
     * Write your selenium wrappers here
     */

    static ObjectMapper mapper = new ObjectMapper();

    /*
     * Common method to go to URL and check if the page is loaded properly using
     * javascript executor readystate
     */
    public static void goToURL(WebDriver driver, String Url) throws InterruptedException {
        driver.get(Url);
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                        .equals("complete"));
    }

    /* common method to click a button */
    public static void clickButton(WebDriver driver, By Locator) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(Locator));
            WebElement clickElement = driver.findElement(Locator);
            clickElement.click();
        } catch (Exception e) {
            System.out.println("Exception message : " + e.getMessage());
        }
    }

    /* method for Testcase01 to get the Team details */
    public static void teamDetails(WebDriver driver, By locator) throws InterruptedException, ParseException {

        /* Creating a ArrayList of HashMap */
        ArrayList<HashMap<String, Object>> teamList = new ArrayList<>();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//ul[@class='pagination']/li/a)[1]")));

        WebElement pagination = driver.findElement(By.xpath("(//ul[@class='pagination']/li/a)[1]"));
        pagination.click();

        for (int page = 1; page <= 4; page++) {
            List<WebElement> parentElement = driver.findElements(locator);
            for (WebElement parent : parentElement) {
                String teamName = parent.findElement(By.xpath("./td[@class='name']")).getText();
                int teamYear = Integer.parseInt(parent.findElement(By.xpath("./td[@class='year']")).getText());
                double teamWinPercent = Double
                        .parseDouble(parent.findElement(By.xpath("./td[contains(@class,'pct')]")).getText());

                long epoch = System.currentTimeMillis() / 1000;
                String teamEpochTime = String.valueOf(epoch);

                if (teamWinPercent < 0.40) {
                    HashMap<String, Object> dataMap = new HashMap<>();
                    /* Putting the value in the map */
                    dataMap.put("epochTime", teamEpochTime);
                    dataMap.put("TeamwinPercent", teamWinPercent);
                    dataMap.put("TeamName", teamName);
                    dataMap.put("TeamYear", teamYear);
                    teamList.add(dataMap);
                }
            }
            /* Clicking on the next page */
            if (page < 4) {
                WebElement nextPageElement = driver.findElement(By.xpath("//a[@aria-label='Next']"));
                nextPageElement.click();
                /* wait till the page loads to scrape the data again */
                Thread.sleep(3000);
            }
        }

        /* Printing the value from the map through the mapper object */
        try {
            String employeePrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(teamList);
            System.out.println(employeePrettyJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        String userDir = System.getProperty("user.dir");

        // Writing JSON on a file
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(userDir + "\\src\\test\\resources\\JSONFromHockeyList.json"), teamList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * method for Testcase02 to get the Oscar Film details for each year from 2015
     * to 2010
     */
    public static void checkOscarFilm(String year, WebDriver driver) throws InterruptedException, ParseException {

        WebElement yearLink = driver.findElement(By.id(year));
        String yearLinkText = yearLink.getText();
        yearLink.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[@class='table']")));

        ArrayList<HashMap<String, String>> filmInfoList = new ArrayList<>();

        /* Getting the values in the table for the clicked year */
        /* getting only the first 5 Film details */

        List<WebElement> filmrows = driver.findElements(By.xpath("//tr[@class='film']"));
        int count = 1;

        for (WebElement film : filmrows.subList(0, 5)) {

            String filmTitle = film.findElement(By.xpath("./td[contains(@class,'title')]")).getText();
            String filmNomination = film.findElement(By.xpath("./td[contains(@class,'nominations')]")).getText();
            String filmAwards = film.findElement(By.xpath("./td[contains(@class,'awards')]")).getText();

            boolean isWinnerflag = count == 1;
            String isWinner = String.valueOf(isWinnerflag);

            long epoch = System.currentTimeMillis() / 1000;
            String filmEpochTime = String.valueOf(epoch);

            HashMap<String, String> filmMap = new HashMap<>();

            /* Adding the Film details to the list */
            filmMap.put("epochTime", filmEpochTime);
            filmMap.put("Year", yearLinkText);
            filmMap.put("Title", filmTitle);
            filmMap.put("Nomination", filmNomination);
            filmMap.put("Awards", filmAwards);
            filmMap.put("isWinner", isWinner);

            /* Putting the Film details to the List from Map */
            filmInfoList.add(filmMap);
            count++;

        }

        /* Writing JSON on a file on the output folder */
        try {
            String userDir = System.getProperty("user.dir");
            File jsonFile = new File(userDir + "/src/test/resources/" + year + "-Oscar-Winner-data.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, filmInfoList);
            System.out.println("Json File written to : " + jsonFile.getAbsolutePath());
            /* Assert if the file exist and is not empty */
            Assert.assertTrue(jsonFile.length() != 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
