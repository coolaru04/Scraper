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
    static ArrayList<String> teamInfoList = new ArrayList<>();
    static ArrayList<String> filmInfoList = new ArrayList<>();    
    static ObjectMapper mapper = new ObjectMapper();
    static Map<String, Object> teamMap = new HashMap<String, Object>();
    static Map<String, Object> filmMap = new HashMap<String, Object>();

    /* Common method to go to URL and check if the page is loaded properly using javascript executor readystate */
    public static void goToURL(WebDriver driver, String Url) throws InterruptedException {
        driver.get(Url);
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                        .equals("complete"));
    }
    
    /* common method to enter a text */
    public static void enterText(WebDriver driver, By Locator, String enterText) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(Locator));
            WebElement inputElement = driver.findElement(Locator);
            inputElement.clear();
            inputElement.sendKeys(enterText);
            inputElement.sendKeys(Keys.ENTER);
        } catch (Exception e) {
            System.out.println("Exception message : " + e.getMessage());
        }
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

    /* method to find the epoch time */
    public static long createEpochTime() throws ParseException{
        Date today = Calendar.getInstance().getTime();      
        SimpleDateFormat crunchifyFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");        
        String currentTime = crunchifyFormat.format(today);              

        Date date = crunchifyFormat.parse(currentTime);
        long epochTime = date.getTime();       
        return epochTime;
    }

    /*method for Testcase01 to get the Team details */
    public static void teamDetails(WebDriver driver, By locator) throws InterruptedException, ParseException {
   
        List<WebElement> fulltable = driver.findElements(locator);

        /* first for loop to traverse to 4 pages. Already 1 page is clicked so starting from page 2 */
        /* Second loop will go through all the elements in the full table */
        for (int j = 2; j <= 5; j++) {
            for (int i = 2; i < fulltable.size(); i++) {
                WebElement teamWinPercent = driver.findElement(By.xpath("//tbody/tr[" + i + "]/td[6]"));
                String check = teamWinPercent.getText();
                double convert = Double.parseDouble(check);
                if (convert < 0.40) {
                    WebElement teamNameElement = driver.findElement(By.xpath("//tbody/tr[" + i + "]/td[1]"));
                    WebElement teamYearElement = driver.findElement(By.xpath("//tbody/tr[" + i + "]/td[2]"));

                    String teamName = String.valueOf(teamNameElement.getText());
                    String teamYear = String.valueOf(teamYearElement.getText());
                    String teamWinPer = String.valueOf(convert);
                    /*creating a new arraylist each time after writing the values to map */
                    teamInfoList = new ArrayList<>();
                    
                    long teamEpochTime=Wrappers.createEpochTime();
                    /*Adding the values to list */
                    teamInfoList.add(String.valueOf(teamEpochTime));
                    teamInfoList.add(teamWinPer);
                    teamInfoList.add(teamName);
                    teamInfoList.add(teamYear);
                    teamInfoList.add(teamWinPer);                   

                    /* Putting the value in the map */
                    teamMap.put(teamName, teamInfoList);

                    // System.out.println("Team Name: "+teamName);
                    // System.out.println("Team Year: "+teamYear);
                    // System.out.println("Teams win% less than 40% : "+convert);                   

                }
            }
            /*Clicking on the next page */
            WebElement pagination = driver.findElement(By.xpath("//a[@href='/pages/forms/?page_num=" + j + "']"));
            pagination.click();
            Thread.sleep((new java.util.Random().nextInt(3) + 2) * 1000);
        }
        /* Printing the value from the map through the mapper object */
        try {
            String employeePrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(teamMap);
            System.out.println(employeePrettyJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        String userDir = System.getProperty("user.dir");

        // Writing JSON on a file
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(userDir + "\\src\\test\\resources\\JSONFromMap.json"), teamMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*method for Testcase02 to get the Oscar Film details for each year from 2015 to 2010*/
    public static void checkOscarFilm(WebDriver driver, By locator) throws InterruptedException, ParseException {

        List<WebElement> listYear = driver.findElements(locator);
        filmInfoList = new ArrayList<>();
        /*Boolean variable to check if the film is the oscar winner for the current year */
        Boolean isWinner;

        /* Loop traverse from 2015 to 2010 */
        for (int i=0;i<listYear.size();i++){
           
            listYear.get(i).click();  
            Thread.sleep(1000);
            /*Getting the current clicked year */
            String year=listYear.get(i).getText();
            WebDriverWait wait=new WebDriverWait(driver,Duration.ofSeconds(10)); 
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tbody/tr")));
            
            /*Getting the values in the table for the clicked year */
            List<WebElement> oscarTable=driver.findElements(By.xpath("//tbody/tr"));      
           
            for(int j=1;j<oscarTable.size();j++){
                isWinner=false;
                filmInfoList = new ArrayList<>();
                /*getting only the first 5 Film details */
                if(j==6){
                    break;
                }
                Thread.sleep(1000);

                String filmTitle=driver.findElement(By.xpath("//tbody/tr[" + j +"]/td[1]")).getText();
                String filmNomination=driver.findElement(By.xpath("//tbody/tr[" + j +"]/td[2]")).getText();
                String filmAwards=driver.findElement(By.xpath("//tbody/tr[" + j +"]/td[3]")).getText(); 
                if(j==1){                   
                  WebElement filmBestPicture=driver.findElement(By.xpath("//tr[" + j +"]/td[4]/i[@class='glyphicon glyphicon-flag']"));
                  if(filmBestPicture.isDisplayed()){                              
                        isWinner=true;                       
                    }             
                }
                long teamEpochTime=Wrappers.createEpochTime();
                /*Adding the Film details to the list */
                filmInfoList.add(String.valueOf(teamEpochTime));
                filmInfoList.add(String.valueOf(year));
                filmInfoList.add(String.valueOf(filmTitle));
                filmInfoList.add(String.valueOf(filmNomination));
                filmInfoList.add(String.valueOf(filmAwards));
                filmInfoList.add(String.valueOf(isWinner));        
                
                /*Putting the Film details to the Map from List - Key is film Title*/
                filmMap.put(filmTitle,filmInfoList);
                //  for (Map.Entry<String, Object> entry : filmMap.entrySet()) {
                // System.out.println(entry.getKey() + ":" + entry.getValue().toString());
                //   }                
            }  
                        
        }

        /*Writing the details of map on console using mapper object */
        try {
            String oscarDetailsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filmMap);
            System.out.println(oscarDetailsJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        String userDir = System.getProperty("user.dir");

        /* Writing JSON on a file on the output folder*/
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(userDir + "\\JSONFromFilmMap.json"), filmMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
      
        /* Assert if the file exist and is not empty */
      Assert.assertFalse("userDir+\\JSONFromFilmMap.json".isEmpty());      
         
    }
}
