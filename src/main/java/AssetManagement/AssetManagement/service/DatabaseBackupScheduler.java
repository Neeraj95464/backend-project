package AssetManagement.AssetManagement.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class DatabaseBackupScheduler {

    @Value("${db.host}")
    private String dbHost;

    @Value("${db.port}")
    private String dbPort;

    @Value("${db.name}")
    private String dbName;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Value("${backup.directory}")
    private String backupDirectory;

    @Value("${mysqldump.path}")
    private String mysqldumpPath;


    @PostConstruct
    public void checkProperties() {
        System.out.println("DB Host: " + dbHost);
        System.out.println("DB Port: " + dbPort);
        System.out.println("DB Name: " + dbName);
        System.out.println("DB User: " + dbUsername);
        System.out.println("DB Password: " + dbPassword);
        System.out.println("Backup Dir: " + backupDirectory);
        System.out.println("mysqldump Path: " + mysqldumpPath);
    }

    // Runs at 7 AM and 7 PM every day
    @Scheduled(cron = "0 0 7,19 * * *")
//    @Scheduled(cron = "0 * * * * *")
    public void backupDatabase() {
        try {
            File backupDir = new File(backupDirectory);
            if (!backupDir.exists()) {
                backupDir.mkdirs(); // Create directory if not exists
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupFile = backupDirectory + File.separator + "backup_" + timestamp + ".sql";

            ProcessBuilder pb = new ProcessBuilder(
                    mysqldumpPath,
                    "-h", dbHost,
                    "-P", dbPort,
                    "-u", dbUsername,
                    "-p" + dbPassword,
                    dbName,
                    "-r", backupFile
            );

            Process process = pb.start();
            int processComplete = process.waitFor();

            if (processComplete == 0) {
                System.out.println("✅ Backup successful: " + backupFile);
            } else {
                System.err.println("❌ Backup failed.");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }




}


