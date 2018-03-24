package org.metafetish.buttplug.core.Messages;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.metafetish.buttplug.core.ButtplugConsts;
import org.metafetish.buttplug.core.ButtplugMessage;

@JsonPropertyOrder({"Id"})
public class StartScanning extends ButtplugMessage {

    @SuppressWarnings("unused")
    private StartScanning() {
        super(ButtplugConsts.DefaultMsgId);
    }

    public StartScanning(long id) {
        super(id);
    }
}
