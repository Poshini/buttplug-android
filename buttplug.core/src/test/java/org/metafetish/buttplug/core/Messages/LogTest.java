package org.metafetish.buttplug.core.Messages;

import org.junit.Assert;
import org.junit.Test;
import org.metafetish.buttplug.core.ButtplugJsonMessageParser;
import org.metafetish.buttplug.core.ButtplugLogLevel;
import org.metafetish.buttplug.core.ButtplugMessage;

import java.io.IOException;
import java.util.List;

public class LogTest {

    @Test
    public void test() throws IOException {
        String testStr = "[{\"Log\":{\"Id\":7,\"LogLevel\":\"Warn\",\"LogMessage\":\"TestLog\"}}]";

        ButtplugJsonMessageParser parser = new ButtplugJsonMessageParser();
        List<ButtplugMessage> msgs = parser.parseJson(testStr);

        Assert.assertEquals(msgs.size(), 1);
        Assert.assertEquals(msgs.get(0).getClass(), Log.class);
        Assert.assertEquals(msgs.get(0).id, 7);
        Assert.assertEquals(((Log) msgs.get(0)).logLevel, ButtplugLogLevel.WARN);
        Assert.assertEquals(((Log) msgs.get(0)).logMessage, "TestLog");

        String jsonOut = parser.formatJson(msgs);
        Assert.assertEquals(testStr, jsonOut);

        jsonOut = parser.formatJson(msgs.get(0));
        Assert.assertEquals(testStr, jsonOut);
    }

}
