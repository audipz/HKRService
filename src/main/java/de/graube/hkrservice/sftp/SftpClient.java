package de.graube.hkrservice.sftp;


import com.jcraft.jsch.*;

/**
 * Einfache JSch-basierte SFTP-Upload-Komponente.
 */
public class SftpClient {

    /**
     * Laedt Inhalt als Datei auf den konfigurierten SFTP-Server.
     *
     * @param content Dateiinhalt
     * @throws Exception bei Verbindungs- oder Uploadfehlern
     */
    public void upload(String content) throws Exception {

        JSch jsch = new JSch();
        Session session = jsch.getSession("user", "host", 22);
        session.setPassword("pass");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        sftp.put(new java.io.ByteArrayInputStream(content.getBytes()), "file.dat");

        sftp.exit();
        session.disconnect();
    }
}
