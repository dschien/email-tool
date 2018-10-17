package net.spe;


import org.apache.commons.csv.CSVRecord;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for simple EmailTool.
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailToolTest {


    @Mock
    CSVRecord recordMock;

    @Test
    public void testIterateEmail() throws EmailException {

        EmailTool emailTool = new EmailTool();
        emailTool.isDryRun = true;

        Properties propertiesMock = mock(Properties.class);
        when(propertiesMock.getProperty("from")).thenReturn("test@me.com");

        when(recordMock.get("Email")).thenReturn("test@you.com");


        Email email = emailTool.iterateEmailData("Test subject",
                propertiesMock,
                "Hi {{firstName}}",
                new HashMap<>(Map.of("firstName", "Daniel")),
                recordMock);

        assertEquals("Test subject", email.getSubject());

    }
}
