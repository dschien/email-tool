package net.spe;

import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.ItemId;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.ItemView;
import picocli.CommandLine;

import java.net.URI;
import java.util.*;

import static net.spe.EmailTool.loadProperties;

@CommandLine.Command(description = "Send emails via exchange",
        name = "emails", mixinStandardHelpOptions = true, version = "emailTemplates 0.1")
public class EWS {

    private static ExchangeService service;
    private static Integer NUMBER_EMAILS_FETCH = 5; // only latest 5 emails/appointments are fetched.

    static {
        try {
            service = new ExchangeService(ExchangeVersion.Exchange2010_SP1);
//            service.setUrl(new URI("https://outlook.office365.com/EWS/exchange.asmx"));
            service.autodiscoverUrl("noreply@bristol.ac.uk", new RedirectionUrlCallback());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @CommandLine.Option(names = {"-s", "--smtp-properties"}, description = "path to property containing host, uname, password")
    private String smtpPropertyPath = "smtp.office365.properties";

    /**
     * Initialize the Exchange Credentials.
     */
    public EWS() throws Exception {
        Properties smtpProperties = loadProperties(System.getProperty("user.dir") + "/" + smtpPropertyPath);
        ExchangeCredentials credentials = new WebCredentials("User Name", "Password");
        service.setCredentials(credentials);
    }

    /**
     * Reading one email at a time. Using Item ID of the email. Creating a
     * message data map as a return value.
     */
    public Map readEmailItem(ItemId itemId) {
        Map messageData = new HashMap();
        try {
            Item itm = Item.bind(service, itemId, PropertySet.FirstClassProperties);
            EmailMessage emailMessage = EmailMessage.bind(service, itm.getId());
            messageData.put("emailItemId", emailMessage.getId().toString());
            messageData.put("subject", emailMessage.getSubject());
            messageData.put("fromAddress", emailMessage.getFrom().getAddress());
            messageData.put("senderName", emailMessage.getSender().getName());
            Date dateTimeCreated = emailMessage.getDateTimeCreated();
            messageData.put("SendDate", dateTimeCreated.toString());
            Date dateTimeRecieved = emailMessage.getDateTimeReceived();
            messageData.put("RecievedDate", dateTimeRecieved.toString());
            messageData.put("Size", emailMessage.getSize() + "");
            messageData.put("emailBody", emailMessage.getBody().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messageData;
    }
    /**
     * Number of email we want to read is defined as NUMBER_EMAILS_FETCH,
     */
    public List<Map> readEmails() {
        List<Map> msgDataList = new ArrayList<>();
        try {
            service.setTraceEnabled(true);
            System.out.println("|---------------------> service = {}" + service);
            Folder folder = Folder.bind(service, WellKnownFolderName.Inbox);
            FindItemsResults<Item> results = service.findItems(folder.getId(), new ItemView(NUMBER_EMAILS_FETCH));
            int i = 1;
            for (Item item : results) {
                Map messageData = readEmailItem(item.getId());
                System.out.println("|---------------------> service = {}" + (i++) + ":");
                System.out.println("|---------------------> service = {}" + messageData.get("subject").toString());
                System.out.println("|---------------------> service = {}" + messageData.get("senderName").toString());
                msgDataList.add(messageData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msgDataList;
    }
    public static void main(String[] args) throws Exception {
        EWS msees = new EWS();
        List<Map> emails = msees.readEmails();
        System.out.println("|---------------------> service = {}" + emails.size());
    }
    static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
        public boolean autodiscoverRedirectionUrlValidationCallback(
                String redirectionUrl) {
            return redirectionUrl.toLowerCase().startsWith("https://");
        }
    }
}