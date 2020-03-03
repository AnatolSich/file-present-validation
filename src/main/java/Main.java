
import exceptions.ValidationException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.text.StringSubstitutor;
import org.apache.log4j.PropertyConfigurator;
import service.CloudStorageClient;
import service.TimeService;
import service.WebhookClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@SuppressWarnings("WeakerAccess")

@CommonsLog
public class Main {

    private static final String PROPS_PATH = "/ibl-bank-file-present-validation/configs/";

    public static void main(String[] args) {

        try {
            Properties appProps = loadProperties();
            WebhookClient webhookClient = new WebhookClient(appProps);

            String symbol = getSymbol(appProps);
            log.info("Symbol = " + symbol);

            TimeService timeService = new TimeService(appProps);
            String fileNameDate = timeService.getCheckDay();
            log.info("fileNameDate = " + fileNameDate);

            String fileName = constructFileName(appProps, symbol, fileNameDate);
            log.info("fileName = " + fileName);


            String fileExtension = getFileExtension(appProps);
            log.info("FileExtension = " + fileExtension);


            CloudStorageClient cloudStorageClient = new CloudStorageClient(appProps, fileExtension);

            try {
                cloudStorageClient.isFileExisted(fileName);
                throw new ValidationException("File for token " + symbol + " exists");
            } catch (ValidationException ex) {
                webhookClient.sendMessageToSlack(ex.getMessage());
            }
            //upload report to AWS
            cloudStorageClient.uploadReportLogToAws(timeService.getLogFileName());
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }

    }

    private static Properties loadProperties() throws IOException {
        File fileApp = new File(PROPS_PATH + "application.properties");
        log.info("Is external app properties exist = " + fileApp.exists());
        InputStream appPath;
        if (fileApp.exists()) {
            appPath = new FileInputStream(fileApp.getPath());
        } else {
            appPath = Main.class.getResourceAsStream("/application.properties");
        }
        Properties appProps = new Properties();
        appProps.load(appPath);
        improveAppProperties(appProps);

        File fileLog = new File(PROPS_PATH + "log4j.properties");
        log.info("Is external log properties exist = " + fileLog.exists());
        InputStream logPath;
        if (fileLog.exists()) {
            logPath = new FileInputStream(fileLog.getPath());
        } else {
            logPath = Main.class.getResourceAsStream("/log4j.properties");
        }
        Properties logProps = new Properties();
        logProps.load(logPath);
        improveLogProperties(appProps, logProps);
        PropertyConfigurator.configure(logProps);
        log.info("AppProps ready");
        return appProps;
    }

    @SuppressWarnings("DuplicatedCode")
    private static void improveAppProperties(Properties appProps) {
        Set<Map.Entry<Object, Object>> set = appProps.entrySet();
        @SuppressWarnings("unchecked")
        StringSubstitutor sub = new StringSubstitutor((Map) appProps);
        for (Map.Entry<Object, Object> entry : set
        ) {
            appProps.replace(entry.getKey(), sub.replace(entry.getValue()));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private static void improveLogProperties(Properties appProps, Properties logProps) {
        appProps.setProperty("token", getSymbol(appProps));
        Set<Map.Entry<Object, Object>> set = logProps.entrySet();
        @SuppressWarnings("unchecked")
        StringSubstitutor sub = new StringSubstitutor((Map) appProps);
        for (Map.Entry<Object, Object> entry : set
        ) {
            logProps.replace(entry.getKey(), sub.replace(entry.getValue()));
        }
    }

    private static String getFileExtension(Properties appProps) {
        String prop = appProps.getProperty("candle-validation.fileExtension");
        if (prop == null || prop.isBlank()) {
            return ".txt";
        } else return prop.trim();
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static String getSymbol(Properties appProps) {
        String prop = appProps.getProperty("aws.s3.loaded.folder.name");
        String[] splitProp = prop.split("/");
        List<String> folders = new ArrayList<>();
        for (int i = 0; i < splitProp.length; i++) {
            String str = splitProp[i].trim();
            if (!str.isBlank()) {
                folders.add(str);
            }
        }
        String symbol = folders.get(folders.size() - 1);
        return String.valueOf(symbol.charAt(0)).toUpperCase() + symbol.substring(1);
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    public static String constructFileName(Properties appProps, String symbol, String dateStr) {
        String fileNameProp = appProps.getProperty("candle-validation.fileName");
        if (fileNameProp != null && !fileNameProp.isBlank()) {
            return fileNameProp;
        }
        StringBuilder fileName = new StringBuilder();
        fileName.append(String.valueOf(symbol.charAt(0)).toUpperCase())
                .append(symbol.substring(1))
                .append("_")
                .append(dateStr)
                .append("_1");
        return fileName.toString();
    }

}
