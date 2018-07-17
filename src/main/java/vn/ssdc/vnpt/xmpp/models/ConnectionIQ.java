package vn.ssdc.vnpt.xmpp.models;

import org.jivesoftware.smack.packet.IQ;

/**
 * Created by THANHLX on 8/10/2017.
 */
public class ConnectionIQ extends IQ {
    public String getChildElementXML(){
        return "<connectionRequest xmlns=\"urn:broadband-forum-org:cwmp:xmppConnReq-1-0\">" +
                "<username>admin</username>" +
                "<password>admin</password>" +
                "</connectionRequest>";
    }
}
