package com.xmail.XmailService;

import com.xmail.Database.QueuedMails;
import org.javalite.activejdbc.Base;

import java.util.List;

/**
 * Created by cristian on 4/30/15.
 */
public class XmailQueue {

    public void init(String dbPath) {

        Base.open("org.sqlite.JDBC", "jdbc:sqlite:" + dbPath, "", "");

        createQueue();
    }

    public void createQueue() {
        Base.exec("CREATE TABLE IF NOT EXISTS queued_mails (\n" +
                "    id             INTEGER (20) PRIMARY KEY\n" +
                "                                NOT NULL,\n" +
                "    mail_from      STRING       NOT NULL,\n" +
                "    mail_to        STRING       NOT NULL,\n" +
                "    email_path     STRING       NOT NULL,\n" +
                "    date_added     DATETIME     NOT NULL,\n" +
                "    date_processed DATETIME     NOT NULL,\n" +
                "    status         INTEGER      NOT NULL,\n" +
                "    retry          INTEGER (1)  NOT NULL,\n" +
                "    mx_ctr         INTEGER (4)  NOT NULL,\n" +
                "    ip_ctr         INTEGER (4)  NOT NULL,\n" +
                "    bind_ip        STRING (39)  NOT NULL,\n" +
                "    last_code      STRING       NOT NULL\n" +
                ");\n");
    }

    public List<QueuedMails> getEmails(int limit) {
        List<QueuedMails> ret = QueuedMails.where("status = 0").limit(limit).orderBy("date_processed asc");
        return ret;
    }

    public void queueEmails() {

    }

    public QueuedMails getEmail(int id) {
        return QueuedMails.findFirst("id = ? ", id);
    }
}