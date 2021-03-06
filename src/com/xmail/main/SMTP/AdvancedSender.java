package com.xmail.main.SMTP;

import com.xmail.main.Dns.Dns;
import com.xmail.main.XmailService.IpQueue;
import com.xmail.main.Config;
import org.apache.log4j.Logger;

import javax.naming.NamingException;

/**
 * Created by cristian on 5/12/15.
 */
public class AdvancedSender extends Sender {
    final static Logger logger = Logger.getLogger(AdvancedSender.class);

    int ipIndex = 0;
    boolean ipv6Used = true;
    int mxIndex = 0;

    String remoteMta;

    public String bindingIPv4 = null;
    public String bindingIPv6 = null;


    /**
     * Sender.send()
     *
     * Send an e-mail using Round Robin MXs
     *
     * @param to
     * @param data
     * @return
     */
    public int send(String to, String data) {
        String[] emailParts = to.split("@");
        String domain;
        IpQueue ipQueue = IpQueue.getInstance();

        if(emailParts.length == 2) {
            domain = emailParts[1];

            // Get all MXes
            try {

                String[][] mxs = Dns.getMX(domain);

                if(mxIndex >= mxs.length) mxIndex = 0;

                for(; mxIndex < mxs.length; mxIndex++) {
                    logger.debug("Remote MTA: " + mxs[mxIndex][1]);

                    if(Config.ipv6Enabled && ipv6Used) {
                        if(bindingIPv6 == null) bindingIPv6 = ipQueue.getIpv6();
                        logger.info("Bound to IPv6: " + bindingIPv6);

                        String[] ips6 = Dns.getAAAA(mxs[mxIndex][1]);
                        int ret = send2IP(to, data, ips6, bindingIPv6);

                        if(ret == SMTP.SERVER_NOT_FOUND || ret == SMTP.MAILBOX_NOT_EXISTS || ret == SMTP.TEMPORARY_ERROR) continue;
                        return ret;
                    }

                    // Try to IPv4
                    ipv6Used = false;
                    if(bindingIPv4 == null) bindingIPv4 = ipQueue.getIpv4();
                    logger.info("Bound to IPv4: " + bindingIPv4);

                    String[] ips4 = Dns.getA(mxs[mxIndex][1]);
                    int ret = send2IP(to, data, ips4, bindingIPv4);

                    if(ret == SMTP.SERVER_NOT_FOUND || ret == SMTP.MAILBOX_NOT_EXISTS || ret == SMTP.TEMPORARY_ERROR) continue;
                    return ret;

                }
            }
            catch (NamingException e) {
            }
        }
        else {
            // TO DO: implement local delivery (ie RCPT TO: root)
        }

        return SMTP.UNKNOWN_ERROR;
    }

    /**
     * Sender.send2IP()
     *
     * Send a mail to all IP addresses of a MX
     *
     * @param to recipient
     * @param data all email data (headers + body)
     * @param ips the list of IP addresses where to try
     * @param sourceIP the source IP address
     * @return SMTP code
     */
    private int send2IP(String to, String data, String[] ips, String sourceIP) {

        if(ipIndex >= ips.length) ipIndex = 0;

        for(; ipIndex < ips.length; ipIndex++) {
            remoteMta = ips[ipIndex];

            boolean status = sendMail(to, data, ips[ipIndex], sourceIP);
            if(status) return SMTP.SUCCESS;

            // parse the last code
            int code = SMTP.parseReturnCode(getLastCode());

            logger.debug("Last code=" + Integer.toString(getLastCode()));

            if(code == SMTP.SERVER_NOT_FOUND) continue;
            else return code;
        }

        // If tried all IPs without success...
        return SMTP.SERVER_NOT_FOUND;
    }

    /**
     * AdvancedSender.getIpIndex()
     *
     * Returns the current index of IP in Round Robin queue
     *
     * @return
     */
    public int getIpIndex() {
        return ipIndex;
    }

    /**
     * AdvancedSender.getMxIndex()
     *
     * Returns the current index of MX in Round Robin queue
     *
     * @return
     */
    public int getMxIndex() {
        return mxIndex;
    }

    /**
     * AdvancedSender.isIpv6Used()
     *
     * Returns if the last connection was open on an IPv6 socket or not
     *
     * @return
     */
    public boolean isIpv6Used() {
        return ipv6Used;
    }

    /**
     * AdvancedSender.setIpIndex()
     *
     * Set the current index for IP
     *
     * @param index
     */
    public void setIpIndex(int index) {
        ipIndex = index;
    }

    /**
     * AdvancedSender.setMxIndex()
     *
     * Set the current index for MX
     *
     * @param index
     */
    public void setMxIndex(int index) {
        mxIndex = index;
    }

    /**
     *
     * AdvancedSender.setIpv6Used()
     *
     * Force to use IPv6
     *
     * @param usage
     */
    public void setIpv6Used(boolean usage) {
        ipv6Used = usage;
    }

    /**
     * AdvancedSender.getIpv4()
     *
     * Returns the used IPv4 address
     *
     * @return
     */
    public String getIpv4() {
        return bindingIPv4;
    }

    /**
     * AdvancedSender.getIpv6()
     *
     * Returns the used IPv6 address
     *
     * @return
     */
    public String getIpv6() {
        return bindingIPv6;
    }

    /**
     * AdvancedSender.getRemoteMta()
     *
     * Returns the IP of the last MTA
     *
     * @return
     */
    public String getRemoteMta() {
        return remoteMta;
    }
}