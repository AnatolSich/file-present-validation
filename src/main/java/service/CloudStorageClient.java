package service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import exceptions.ValidationException;
import lombok.extern.apachecommons.CommonsLog;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

@CommonsLog
public class CloudStorageClient {

    private final String verificationBucketName;
    private final String verificationFolderName;
    private final String reportBucketName;
    private final String reportFolderName;
    private final String fileExtension;
    private final String missingFileMessage;


    private final AmazonS3 s3client;

    public CloudStorageClient(Properties appProps, String fileExtension) {
        this.verificationBucketName = appProps.getProperty("aws.s3.loaded.bucket.name");
        this.verificationFolderName = appProps.getProperty("aws.s3.loaded.folder.name");
        this.reportBucketName = appProps.getProperty("aws.s3.report.bucket.name");
        this.reportFolderName = appProps.getProperty("aws.s3.report.folder.name");
        this.fileExtension = fileExtension;
        this.s3client = buildAmazonClient(appProps);
        this.missingFileMessage = appProps.getProperty("candle-validation.slack.missing_file_message");
    }

    private static AmazonS3 buildAmazonClient(Properties appProps) {
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(appProps.getProperty("aws.access.key"), appProps.getProperty("aws.secret.key"))))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(appProps.getProperty("aws.access.endpoint"), Regions.US_EAST_1.name()))
                .build();
    }

    private boolean checkBucketAndObject(String bucket, String key) {
        return (s3client.doesBucketExistV2(bucket) && s3client.doesObjectExist(bucket, key));
    }

    private String createFilePath(String fileName) throws Exception {
        StringBuilder path = new StringBuilder();
        if (verificationFolderName != null) {
            path.append(verificationFolderName);
        }
        if (fileName != null && !fileName.isBlank()) {
            path.append(fileName);
        } else {
            throw new Exception("No file name sent to download");
        }

        if (fileExtension != null) {
            path.append(fileExtension);
        }
        return path.toString();
    }

    @SuppressWarnings("SameReturnValue")
    public void isFileExisted(String key) throws Exception {
        String path = createFilePath(key);
        log.info("AWS path to file = " + path);
        boolean result = checkBucketAndObject(verificationBucketName, path);
        if (!result) {
            log.info("IsFileExisted = " + false);
            throw new ValidationException(String.format(missingFileMessage, path));
        }
        log.info("IsFileExisted = " + true);
    }

    public void uploadReportLogToAws(String path) throws Exception {
        String key = Path.of(path).getFileName().toString();
        if (reportBucketName == null) {
            log.info(key + " is NOT uploaded to AWS");
            return;
        }
        if (!s3client.doesBucketExistV2(reportBucketName)) {
            throw new Exception("There's no such AWS bucket \"" + reportBucketName.substring(1) + "\" to upload report");
        }
        if (reportFolderName != null && !reportFolderName.isBlank()) {
            key = reportFolderName + "/" + key;
        }
        File file = new File(path);
        s3client.putObject(reportBucketName, key, file);
        log.info(key + " is uploaded to AWS");
    }

}