import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.List;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MKTfinder {

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/MKTproject/MKTCheck/chromedriver-mac-x64/chromedriver");
        WebDriver driver = new ChromeDriver();
        checkCourts(driver);
        driver.quit();
    }

    public static void checkCourts(WebDriver driver) {
        StringBuilder body = new StringBuilder();
        List<String> dateList = new ArrayList<>();
        Date date = new Date();

        //Loop to add current date and next 5 days
        for (int i = 0; i < 4; i++) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDate = formatter.format(date);
            dateList.add(formattedDate);
            date = new Date(date.getTime() + 24 * 60 * 60 * 1000);
        }

        System.out.println(dateList);

        for (int i = 0; i < 4; i++) {
            String mktURL = "https://www.rezerwujkort.pl/klub/mkt_lodz/rezerwacja_kortu_2/" + dateList.get(i) + "/";
            driver.get(mktURL);

            try {
                Thread.sleep(5000); //Sleep for 5 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String dateStr = dateList.get(i);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String dayOfW = "";
            try {
                Date date_ = formatter.parse(dateStr);
                SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                String dayOfWeek = dayFormatter.format(date_);
                dayOfW = dayOfWeek;
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<List<String>> tableMatrix = extractTableToMatrix(driver);

            if (dayOfW.contains("Saturday") || dayOfW.contains("Sunday")){
                List<List<String>> subsetMatrix = extractSubsetMatrix(tableMatrix, 20, 26); // 20/26
                System.out.println(dayOfW + ":\n");
                for (List<String> row : subsetMatrix) {
                    System.out.println(row);
                }
                body.append(dayOfW).append(": \n");
                checkColumns(subsetMatrix, body);
            } else {
                List<List<String>> subsetMatrix = extractSubsetMatrix(tableMatrix, 22, 28);
                System.out.println(dayOfW + ":\n");
                for (List<String> row : subsetMatrix) {
                    System.out.println(row);
                }
                body.append(dayOfW).append(": \n");
                checkColumns(subsetMatrix, body);
            }
        }

        if (body.length() >= 60) {
            sendEmail("example@gmail.com", "MKT courts availability 17:00-20:30", body.toString());
        }
    }

    public static List<List<String>> extractTableToMatrix(WebDriver driver) {
        List<List<String>> matrix = new ArrayList<>();
        List<WebElement> rows = driver.findElements(By.xpath("//tr"));

        for (WebElement row : rows) {
            List<String> rowValues = new ArrayList<>();
            List<WebElement> tds = row.findElements(By.xpath(".//td[contains(@class, 'td-display')]"));

            for (WebElement td : tds) {
                WebElement div = td.findElement(By.xpath(".//div[contains(@class, 'availabilty-field')]"));
                if (div != null) {
                    rowValues.add(div.getText().trim());
                }
            }
            if (!rowValues.isEmpty()) {
                matrix.add(rowValues);
            }
        }
        return matrix;
    }

    public static List<List<String>> extractSubsetMatrix(List<List<String>> matrix, int startRow, int endRow) {
        int matrixSize = matrix.size();
        if (startRow < 0 || endRow >= matrixSize || startRow > endRow) {
            throw new IllegalArgumentException("Invalid row range specified.");
        }

        List<List<String>> subset = new ArrayList<>();
        for (int i = startRow; i <= endRow; i++) {
            subset.add(matrix.get(i));
        }

        return subset;
    }

    public static void sendEmail(String to, String subject, String content) {
        final String username = "example@gmail.com";
        final String password = "password";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        //props.put("mail.debug", "true");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);

            System.out.println("Email sent successfully.");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void checkColumns(List<List<String>> matrix, StringBuilder body) {
        int numRows = matrix.size();
        int numCols = matrix.get(0).size();

        for (int col = 0; col < numCols; col++) {
            int availabilityCount = 0;
            for (int row = 0; row < numRows; row++) {
                if (matrix.get(row).get(col).equals("dostępny")) {
                    availabilityCount++;
                }
            }
            if (availabilityCount == 3) {
                int collumn = col + 1;
                body.append(" - court #").append(collumn).append(" available for 1.5h.\n");
                System.out.println(body);

            } else if (availabilityCount > 3) {
                int collumn = col + 1;
                body.append(" - court #").append(collumn).append(" available for at least 2h.\n");
                System.out.println(body);
            }
        }
    }
}