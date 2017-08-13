package org.metafetish.buttplug.core.Messages;

import org.metafetish.buttplug.core.ButtplugConsts;
import org.metafetish.buttplug.core.ButtplugMessage;

public class StopScanning extends ButtplugMessage {

    public StopScanning() {
        super(ButtplugConsts.DefaultMsgId);
    }

    public StopScanning(long id) {
        super(id);
    }
}
