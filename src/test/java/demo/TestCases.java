package demo;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.logging.Level;
// import io.github.bonigarcia.wdm.WebDriverManager;
import demo.wrappers.Wrappers;

public class TestCases {
    ChromeDriver driver;

    /*
     * TODO: Write your tests here with testng @Test annotation. 
     * Follow `testCase01` `testCase02`... format or what is provided in instructions
     */

     
    /*
     * Do not change the provided methods unless necessary, they will help in automation and assessment
     */
    @BeforeTest
    public void startBrowser()
    {
        System.setProperty("java.util.logging.config.file", "logging.properties");

        // NOT NEEDED FOR SELENIUM MANAGER
        // WebDriverManager.chromedriver().timeout(30).setup();

        ChromeOptions options = new ChromeOptions();
        LoggingPreferences logs = new LoggingPreferences();

        logs.enable(LogType.BROWSER, Level.ALL);
        logs.enable(LogType.DRIVER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logs);
        options.addArguments("--remote-allow-origins=*");

        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, "build/chromedriver.log"); 

        driver = new ChromeDriver(options);

        driver.manage().window().maximize();
    }


        @BeforeMethod
        public void scrapperlogin() throws InterruptedException{
                Wrappers.goToURL(driver, "https://www.scrapethissite.com/pages/");
                Thread.sleep((new java.util.Random().nextInt(3)+2) * 1000); //1000 - 4000ms
        }

       /* testCase01: Go to this website and click on "Hockey Teams: Forms, Searching and Pagination" */
        @Test(enabled = true)
        public void testCase01() throws InterruptedException, ParseException {
                System.out.println("Start TestCase01 ");   
                /*Clicking the guide button */
                By hockeylink=By.xpath("//a[@href='/pages/forms/']");
                Wrappers.clickButton(driver, hockeylink);
                By tabledetails=By.xpath("//tbody/tr");
                Wrappers.teamDetails(driver, tabledetails);
                System.out.println("End TestCase01");
        }

        /*testCase02: Go to this website and click on "Oscar Winning Films" */
        @Test(enabled = true)
        public void testCase02() throws InterruptedException, ParseException {
                System.out.println("Start TestCase02 ");   
                /*Clicking the guide button */
                By oscarLink=By.xpath("//a[@href='/pages/ajax-javascript/']"); 
                Wrappers.clickButton(driver, oscarLink);
                By yearLink=By.xpath("//a[@href='#']");
                Wrappers.checkOscarFilm(driver, yearLink);
                System.out.println("End TestCase02");
        }

    @AfterTest
    public void endTest()
    {
        driver.close();
        driver.quit();

    }
}