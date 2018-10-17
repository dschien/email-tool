package net.spe;

import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Hello world!
 */
@CommandLine.Command(description = "Send emails from templates",
        name = "emails", mixinStandardHelpOptions = true, version = "emailTemplates 0.1")

public class EmailTool implements Callable<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(EmailTool.class);

    static Properties loadProperties(String path) throws IOException {
        Properties appProps = new Properties();
        appProps.load(new FileInputStream(path));

        return appProps;

    }

    @CommandLine.Option(names = {"-d", "--dry-run"}, description = "do not send emails out")
    private boolean isDryRun = false;


    @CommandLine.Option(names = {"-s", "--smtp-properties"}, description = "path to property containing host, uname, password")
    private String smtpPropertyPath = "smtp.properties";

    @CommandLine.Option(names = {"-p", "--template-properties"}, description = "path to property containing common content")
    private String templatePropertyPath = "template.properties";

    @CommandLine.Option(names = {"-t", "--template"}, description = "path to html template")
    private String templatePath = "email_template.html";

    @CommandLine.Option(names = {"-c", "--csv-file"}, description = "path to cvs file with addressees")
    private String emailDataCSVPath = "email_data.csv";


    public static void main(String[] args) throws IOException, EmailException {
        CommandLine.call(new EmailTool(), args);
    }

    @Override
    public Void call() throws IOException, EmailException {

        LOG.info("Loading common template data from " + templatePropertyPath);
        Properties templateProperties = loadProperties(System.getProperty("user.dir") + "/" + templatePropertyPath);

        Jinjava jinjava = new Jinjava();
        Map<String, Object> context = Maps.newHashMap();

        for (String p : templateProperties.stringPropertyNames()) {
            context.put(p, templateProperties.getProperty(p));
        }


        LOG.info("Loading template from " + templatePropertyPath);
        byte[] fileBytes = Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "/" + templatePath));
        String template = new String(fileBytes);


        String renderedTemplate = null;

        Properties smtpProperties = loadProperties(System.getProperty("user.dir") + "/" + smtpPropertyPath);

        LOG.info("Loading list of template values from " + templatePropertyPath);
        Reader in = new FileReader(emailDataCSVPath);
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);

        for (CSVRecord record : records) {

            String emailAddress = record.get("Email");
            String firstName = record.get("First Name");
            String program = record.get("Program");

            context.put("firstName", firstName);
            context.put("program", program);

            renderedTemplate = jinjava.render(template, context);
            Email email = buildEmail(smtpProperties.getProperty("smtpHost"), smtpProperties.getProperty("password"), smtpProperties.getProperty("username"), smtpProperties.getProperty("from"));
            String subject = templateProperties.getProperty("subject");
            LOG.debug("Setting subject: " + subject);
            email.setSubject(subject);

            LOG.debug("Setting Msg: " + renderedTemplate);
            email.setMsg(renderedTemplate);
            email.addTo(emailAddress);

            if (!isDryRun) {
                LOG.info("Sending email to " + emailAddress);
                email.send();
            }
        }

        return null;

    }

    static Email buildEmail(String smtpHostName, String password, String username, String fromEmail) throws EmailException {
        Email email = new HtmlEmail();
        email.setHostName(smtpHostName);
        email.setSmtpPort(465);
        email.setAuthenticator(new DefaultAuthenticator(username, password));
        email.setSSLOnConnect(true);
        email.setFrom(fromEmail);
        return email;
    }

}
