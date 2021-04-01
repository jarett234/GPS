package ca.nylus.nylusgps;

import org.json.JSONException;
import org.json.JSONObject;

public class LoggingManager {

    private IOTMessageManager _messagingManager;
    private final String MESSAGE_TYPE = "logging";
    private String msgStr;
    private String _deviceId = "";

    public LoggingManager(IOTMessageManager iotMessageManager, String deviceId){
        _messagingManager = iotMessageManager;
        _deviceId = deviceId;
    }

    public void SendLog(String logMessage) {
        if(_messagingManager.IsOpen()){
            JSONObject jsonParam = new JSONObject();
            try {
                jsonParam.put("message", logMessage);
                jsonParam.put("messagetype", MESSAGE_TYPE);
                jsonParam.put("deviceid", _deviceId);
                msgStr = jsonParam.toString();
                _messagingManager.SendMessage(msgStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
