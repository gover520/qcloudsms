package com.qcloud.sms;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

class SmsSenderUtil {

    protected Random random = new Random();
    
    public String stringMD5(String input) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] inputByteArray = input.getBytes();
        messageDigest.update(inputByteArray);
        byte[] resultByteArray = messageDigest.digest();
        return byteArrayToHex(resultByteArray);
    }
    
    protected String strToHash(String str) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] inputByteArray = str.getBytes();
        messageDigest.update(inputByteArray);
        byte[] resultByteArray = messageDigest.digest();
        return byteArrayToHex(resultByteArray);
    }

    public String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }
    
    public int getRandom() {
    	return random.nextInt(999999)%900000+100000;
    }
    
    public HttpURLConnection getPostHttpConn(String url) throws Exception {
        URL object = new URL(url);
        HttpURLConnection conn;
        conn = (HttpURLConnection) object.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestMethod("POST");
        return conn;
	}
    
    public String calculateSig(
    		String appkey,
    		long random,
    		String msg,
    		long curTime,    		
    		ArrayList<String> phoneNumbers) throws NoSuchAlgorithmException {
        String phoneNumbersString = phoneNumbers.get(0);
        for (int i = 1; i < phoneNumbers.size(); i++) {
            phoneNumbersString += "," + phoneNumbers.get(i);
        }
        return strToHash(String.format(
        		"appkey=%s&random=%d&time=%d&mobile=%s",
        		appkey, random, curTime, phoneNumbersString));
    }
    
    public String calculateSigForTempl(
    		String appkey,
    		long random,
    		long curTime,    		
    		ArrayList<String> phoneNumbers) throws NoSuchAlgorithmException {
        String phoneNumbersString = phoneNumbers.get(0);
        for (int i = 1; i < phoneNumbers.size(); i++) {
            phoneNumbersString += "," + phoneNumbers.get(i);
        }
        return strToHash(String.format(
        		"appkey=%s&random=%d&time=%d&mobile=%s",
        		appkey, random, curTime, phoneNumbersString));
    }
    
    public String calculateSigForTempl(
    		String appkey,
    		long random,
    		long curTime,    		
    		String phoneNumber) throws NoSuchAlgorithmException {
    	ArrayList<String> phoneNumbers = new ArrayList<>();
    	phoneNumbers.add(phoneNumber);
    	return calculateSigForTempl(appkey, random, curTime, phoneNumbers);
    }
    
    public JSONArray phoneNumbersToJSONArray(String nationCode, ArrayList<String> phoneNumbers) {
        JSONArray tel = new JSONArray();
        int i = 0;
        do {
            JSONObject telElement = new JSONObject();
            telElement.put("nationcode", nationCode);
            telElement.put("mobile", phoneNumbers.get(i));
            tel.put(telElement);
        } while (++i < phoneNumbers.size());

        return tel;
    }
    
    public JSONArray smsParamsToJSONArray(ArrayList<String> params) {
        JSONArray smsParams = new JSONArray();        
        for (int i = 0; i < params.size(); i++) {
        	smsParams.put(params.get(i));	
		}
        return smsParams;
    }
    
    public SmsSingleSenderResult jsonToSmsSingleSenderResult(JSONObject json) {
    	SmsSingleSenderResult result = new SmsSingleSenderResult();
    	
    	result.result = json.getInt("result");
    	result.errMsg = json.getString("errmsg");
    	if (0 == result.result) {
	    	result.ext = json.getString("ext");
	    	result.sid = json.getString("sid");
	    	result.fee = json.getInt("fee");
    	}
    	return result;
    }
    
    public SmsMultiSenderResult jsonToSmsMultiSenderResult(JSONObject json) {
    	SmsMultiSenderResult result = new SmsMultiSenderResult();
    	
    	result.result = json.getInt("result");
    	result.errMsg = json.getString("errmsg");    	
    	if (false == json.isNull("ext")) {
    		result.ext = json.getString("ext");	
    	}
    	if (0 != result.result) {
    		return result;
    	}
    	
    	result.details = new ArrayList<>();    	
    	JSONArray details = json.getJSONArray("detail");
    	for (int i = 0; i < details.length(); i++) {
    		JSONObject jsonDetail = details.getJSONObject(i);
    		SmsMultiSenderResult.Detail detail = result.new Detail();
    		detail.result = jsonDetail.getInt("result");
    		detail.errMsg = jsonDetail.getString("errmsg");
    		detail.phoneNumber = jsonDetail.getString("mobile");
    		detail.nationCode = jsonDetail.getString("nationcode");
    		if (0 == detail.result) {	    	
	    		if (false == jsonDetail.isNull("sid")) {
	    			detail.sid = jsonDetail.getString("sid");	
	    		}		
	    		detail.fee = jsonDetail.getInt("fee");
    		}
    		result.details.add(detail);
    	}
    	return result;
    }
    
    public SmsStatusPullerResult jsonToSmsStatusPullerResult(JSONObject json) {
    	SmsStatusPullerResult result = new SmsStatusPullerResult();
    	
    	result.result = json.getInt("result");
    	result.errmsg = json.getString("errmsg");    	
    	if (true == json.isNull("data")) {
    		return result;
    	}
    	
    	JSONObject data  = json.getJSONObject("data");
    	result.data = result.new Data();
    	result.data.success = data.getInt("success");
    	result.data.status = data.getInt("status");
    	result.data.status_success = data.getInt("status_success");
    	result.data.status_fail = data.getInt("status_fail");
    	result.data.status_fail_0 = data.getInt("status_fail_0");
    	result.data.status_fail_1 = data.getInt("status_fail_1");
    	result.data.status_fail_2 = data.getInt("status_fail_2");
    	result.data.status_fail_3 = data.getInt("status_fail_3");
    	result.data.status_fail_4 = data.getInt("status_fail_4");
    	return result;
    }
    
    public SmsVoiceUploaderResult jsonToSmsVoiceUploaderResult(JSONObject json) {
    	SmsVoiceUploaderResult result = new SmsVoiceUploaderResult();
    	result.result = json.getInt("result");
    	if (false == json.isNull("msg")) {
    		result.msg = json.getString("msg");
    	}
    	if (0 == result.result) {
    		result.file = json.getString("file");
    	}
    	return result;
    }
    
    public SmsSingleVoiceSenderResult jsonToSmsSingleVoiceSenderResult(JSONObject json) {
    	SmsSingleVoiceSenderResult result = new SmsSingleVoiceSenderResult();
    	result.result = json.getInt("result");
    	if (false == json.isNull("errmsg")) {
    		result.errmsg = json.getString("errmsg");
    	}
    	if (0 == result.result) {    		
    		result.ext = json.getString("ext");
    		result.callid = json.getString("callid");
    	}    	
    	return result;    	
    }
}
