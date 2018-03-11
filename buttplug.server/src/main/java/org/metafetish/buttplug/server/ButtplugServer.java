package org.metafetish.buttplug.server;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.metafetish.buttplug.core.ButtplugEvent;
import org.metafetish.buttplug.core.ButtplugEventHandler;
import org.metafetish.buttplug.core.ButtplugJsonMessageParser;
import org.metafetish.buttplug.core.ButtplugLogManager;
import org.metafetish.buttplug.core.ButtplugMessage;
import org.metafetish.buttplug.core.IButtplugCallback;
import org.metafetish.buttplug.core.IButtplugLog;
import org.metafetish.buttplug.core.IButtplugLogManager;
import org.metafetish.buttplug.core.Messages.Error;
import org.metafetish.buttplug.core.Messages.IButtplugMessageOutgoingOnly;
import org.metafetish.buttplug.core.Messages.Ok;
import org.metafetish.buttplug.core.Messages.Ping;
import org.metafetish.buttplug.core.Messages.RequestLog;
import org.metafetish.buttplug.core.Messages.RequestServerInfo;
import org.metafetish.buttplug.core.Messages.ScanningFinished;
import org.metafetish.buttplug.core.Messages.ServerInfo;
import org.metafetish.buttplug.core.Messages.StopAllDevices;
import org.metafetish.buttplug.core.Messages.Test;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutionException;

public class ButtplugServer {
    @NonNull
    private ButtplugJsonMessageParser parser;

    @Nullable
    public ButtplugEventHandler messageReceived = new ButtplugEventHandler();

    @Nullable
    public ButtplugEventHandler clientConnected = new ButtplugEventHandler();

    @NonNull
    protected IButtplugLogManager bpLogManager;

    @NonNull
    private IButtplugLog bpLogger;

    @NonNull
    private DeviceManager deviceManager;

    private Timer pingTimer;

    private String serverName;
    private long maxPingTime;
    private boolean pingTimedOut;
    private boolean receivedRequestServerInfo;
    @SuppressWarnings("FieldCanBeLocal")
    private long clientMessageVersion;

    public static String getLicense() {
        //TODO: Implement getLicense()
        return "";
    }

    public ButtplugServer(String aServerName, long aMaxPingTime) {
        this(aServerName, aMaxPingTime, null);
    }

    public ButtplugServer(@NonNull String aServerName, long aMaxPingTime, DeviceManager
            aDeviceManager) {
        this.serverName = aServerName;
        this.maxPingTime = aMaxPingTime;
        this.pingTimedOut = false;
        //TODO: Implement pingTimer

        this.bpLogManager = new ButtplugLogManager();
        this.bpLogger = this.bpLogManager.getLogger(this.getClass());
        this.bpLogger.debug("Setting up ButtplugServer");
        this.parser = new ButtplugJsonMessageParser();
        this.deviceManager = aDeviceManager != null ? aDeviceManager : new DeviceManager(this.bpLogManager);

        this.bpLogger.info("Finished setting up ButtplugServer");
        this.deviceManager.deviceMessageReceived.addCallback(this.deviceMessageReceivedCallback);
        this.deviceManager.scanningFinished.addCallback(this.scanningFinishedCallback);
        this.bpLogManager.logMessageReceived.addCallback(this.logMessageReceivedCallback);
    }

    private IButtplugCallback deviceMessageReceivedCallback = new IButtplugCallback() {
        @Override
        public void invoke(ButtplugEvent aEvent) {
            if (ButtplugServer.this.messageReceived != null) {
                ButtplugServer.this.messageReceived.invoke(aEvent);
            }
        }
    };

    private IButtplugCallback logMessageReceivedCallback = new IButtplugCallback() {
        @Override
        public void invoke(ButtplugEvent aEvent) {
            if (ButtplugServer.this.messageReceived != null) {
                ButtplugServer.this.messageReceived.invoke(aEvent);
            }
        }
    };

    private IButtplugCallback scanningFinishedCallback = new IButtplugCallback() {
        @Override
        public void invoke(ButtplugEvent aEvent) {
            if (ButtplugServer.this.messageReceived != null) {
                ButtplugServer.this.messageReceived.invoke(new ButtplugEvent(new ScanningFinished()));
            }
        }
    };

    //TODO: Implement pingTimer

    protected ListenableFuture<ButtplugMessage> sendMessage(ButtplugMessage aMsg) throws
            ExecutionException, InterruptedException, InvocationTargetException,
            IllegalAccessException {
        SettableListenableFuture<ButtplugMessage> promise = new SettableListenableFuture<>();

        this.bpLogger.trace("Got Message " + aMsg.id + " of type " + aMsg.getClass().getSimpleName() + " to send");
        long id = aMsg.id;
        if (id == 0) {
            promise.set(this.bpLogger.logWarnMsg(id, Error.ErrorClass.ERROR_MSG, "Message Id 0 is reserved for outgoing system messages. Please use " + "another Id."));
            return promise;
        }

        if (aMsg instanceof IButtplugMessageOutgoingOnly) {
            promise.set(this.bpLogger.logWarnMsg(id, Error.ErrorClass.ERROR_MSG, "Message of type " + aMsg.getClass().getSimpleName() + " cannot be sent to server"));
            return promise;
        }


        if (this.pingTimedOut) {
            promise.set(this.bpLogger.logWarnMsg(id, Error.ErrorClass.ERROR_PING, "Ping timed out."));
            return promise;
        }

        if (!this.receivedRequestServerInfo && !(aMsg instanceof RequestServerInfo)) {
            promise.set(this.bpLogger.logErrorMsg(id, Error.ErrorClass.ERROR_INIT, "RequestServerInfo must be first message received by server!"));
            return promise;
        }

        if (aMsg instanceof RequestLog) {
            this.bpLogger.debug("Got RequestLog Message");
            this.bpLogManager.setButtplugLogLevel(((RequestLog) aMsg).logLevel);
            promise.set(new Ok(id));
            return promise;
        } else if (aMsg instanceof Ping) {
            //TODO: Implement pingTimer
        } else if (aMsg instanceof RequestServerInfo) {
            this.bpLogger.debug("Got RequestServerInfo Message");
            this.receivedRequestServerInfo = true;
            this.clientMessageVersion = ((RequestServerInfo) aMsg).messageVersion;

            //TODO: Implement pingTimer
            //TODO: clientConnectedListeners event?
            if (this.clientConnected != null) {
                this.clientConnected.invoke(new ButtplugEvent(aMsg));
            }
            promise.set(new ServerInfo(this.serverName, 1, this.maxPingTime, id));
            return promise;
        } else if (aMsg instanceof Test) {
            promise.set(new Test(((Test) aMsg).getTestString(), id));
            return promise;
        }
        promise.set(this.deviceManager.sendMessage(aMsg).get());
        return promise;
    }

    protected ListenableFuture<Void> Shutdown() throws ExecutionException, InterruptedException,
            InvocationTargetException, IllegalAccessException {
        SettableListenableFuture<Void> promise = new SettableListenableFuture<>();
        ButtplugMessage msg = this.deviceManager.sendMessage(new StopAllDevices()).get();
        if (msg instanceof Error) {
            this.bpLogger.error("An error occured while stopping devices on shutdown.");
            this.bpLogger.error(((Error) msg).errorMessage);
        }

        this.deviceManager.stopScanning();
        this.deviceManager.deviceMessageReceived.removeCallback(this.deviceMessageReceivedCallback);
        this.deviceManager.scanningFinished.removeCallback(this.scanningFinishedCallback);
        this.bpLogManager.logMessageReceived.removeCallback(this.logMessageReceivedCallback);
        return promise;
    }

    protected ListenableFuture<List<ButtplugMessage>> sendMessage(String aJsonMsgs) throws
            ExecutionException, InterruptedException, IOException, InvocationTargetException,
            IllegalAccessException {
        SettableListenableFuture<List<ButtplugMessage>> promise = new SettableListenableFuture<>();
        List<ButtplugMessage> msgs = this.parser.parseJson(aJsonMsgs);
        List<ButtplugMessage> res = new ArrayList<>();
        for (ButtplugMessage msg : msgs) {
            if (msg instanceof Error) {
                res.add(msg);
            } else {
                res.add(this.sendMessage(msg).get());
            }
        }
        promise.set(res);
        return promise;
    }

    public String serialize(ButtplugMessage aMsg) throws IOException {
        return this.parser.formatJson(aMsg);
    }

    public String serialize(List<ButtplugMessage> aMsgs) throws IOException {
        return this.parser.formatJson(aMsgs);
    }

    public List<ButtplugMessage> deserialize(String aMsg) throws IOException {
        return this.parser.parseJson(aMsg);
    }

    public void addDeviceSubtypeManager(IDeviceSubtypeManager aMgr) {
        this.deviceManager.addDeviceSubtypeManager(aMgr);
    }

    @NonNull
    DeviceManager getDeviceManager() {
        return this.deviceManager;
    }
}