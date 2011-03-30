/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *  This file is covered by the GNU LESSER GENERAL PUBLIC LICENSE VERSION 3.0    *
 *  A copy of this license is located in file license.txt at the root of this    *
 *  SDK or can be downloaded here:                                               *
 *  http://www.gnu.org/licenses/lgpl-3.0.txt                                     *
 *                                                                               *
 *********************************************************************************/



/*
 *  Copyright(C) 2006 Cameron Rich
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2.1 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * A wrapper around the unmanaged interface to give a semi-decent Java API
 */

package totalcross.net.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.security.auth.x500.X500Principal;

import sun.security.validator.ValidatorException;
import totalcross.Launcher;
import totalcross.net.Socket;
import totalcross.sys.Vm;
import totalcross.util.Hashtable;

/**
 * @defgroup java_api Java API.
 *
 * Ensure that the appropriate dispose() methods are called when finished with
 * various objects - otherwise memory leaks will result.
 */

/**
 * A representation of an SSL connection.
 */
public class SSL
{
   Object ssl;
   Socket socket;
   TrustManager[] _trustMgrs; // skip consistency check
   int status = Constants.SSL_NOT_OK;
   Exception lastException;

   static private final Hashtable cache = new Hashtable(17);

   static void cachePutSSL(Socket s, SSL ssl)
   {
      synchronized(cache)
      {
         if (ssl == null)
            cache.remove(s);
         else
            cache.put(s, ssl);
      }
   }
   static SSL cacheGetSSL(Socket s)
   {
      synchronized(cache)
      {
         return (SSL)cache.get(s);
      }
   }

   /**
     * Store the reference to an SSL context.
     * @param ssl A reference to an SSL object.
     */
   protected SSL(Object ssl, Socket socket)
   {
      this.ssl = ssl;
      this.socket = socket;
      ((SSLSocket)ssl).addHandshakeCompletedListener(new HandshakeCompletedListener() {
         public void handshakeCompleted(HandshakeCompletedEvent arg)
         {
            status = Constants.SSL_OK;
         }
      });
      cachePutSSL(socket, this);
   }

   /**
     * Free any used resources on this connection.
     *
     * A "Close Notify" message is sent on this connection (if possible). It
     * is up to the application to close the socket.
     */
   final public void dispose()
   {
      try
      {
         ((SSLSocket)ssl).close();
         cachePutSSL(socket, null);
      }
      catch (IOException e)
      {
         lastException = e;
         Launcher.print(Vm.getStackTrace(e));
      }
   }

   /**
    * Return the result of a handshake.
    * @return SSL_OK if the handshake is complete and ok.
    */
   final public int handshakeStatus()
   {
      return status;
   }

   /**
    * Return the SSL cipher id.
    * @return The cipher id which is one of:
    * - TLS_RSA_WITH_AES_128_CBC_SHA  (0x2f)
    * - TLS_RSA_WITH_AES_256_CBC_SHA  (0x35)
    * - TLS_RSA_WITH_RC4_128_SHA      (0x05)
    * - TLS_RSA_WITH_RC4_128_MD5      (0x04)
    */
   final public byte getCipherId()
   {
      if (ssl != null)
      {
         String cs = ((SSLSocket)ssl).getSession().getCipherSuite();
         if      (cs.equals("TLS_RSA_WITH_AES_128_CBC_SHA"))  return Constants.TLS_RSA_WITH_AES_128_CBC_SHA;
         else if (cs.equals("TLS_RSA_WITH_AES_256_CBC_SHA"))  return Constants.TLS_RSA_WITH_AES_256_CBC_SHA;
         else if (cs.equals("TLS_RSA_WITH_RC4_128_SHA"))      return Constants.TLS_RSA_WITH_RC4_128_SHA;
         else if (cs.equals("TLS_RSA_WITH_RC4_128_MD5"))      return Constants.TLS_RSA_WITH_RC4_128_MD5;
      }
      return -1;
   }

   /**
    * Get the session id for a handshake.
    *
    * This will be a 32 byte sequence and is available after the first
    * handshaking messages are sent.
    * A SSLv23 handshake may have only 16 valid bytes.
    * @return The session id as a 32 byte sequence.
    */
   final public byte[] getSessionId()
   {
      if (ssl != null)
         return ((SSLSocket)ssl).getSession().getId();
      return null;
   }

   /**
    * Retrieve an X.509 distinguished name component.
    *
    * When a handshake is complete and a certificate has been exchanged, then
    * the details of the remote certificate can be retrieved.
    *
    * This will usually be used by a client to check that the server's common
    * name matches the URL.
    *
    * A full handshake needs to occur for this call to work.
    *
    * @param component [in] one of:
    * - SSL_X509_CERT_COMMON_NAME
    * - SSL_X509_CERT_ORGANIZATION
    * - SSL_X509_CERT_ORGANIZATIONAL_NAME
    * - SSL_X509_CA_CERT_COMMON_NAME
    * - SSL_X509_CA_CERT_ORGANIZATION
    * - SSL_X509_CA_CERT_ORGANIZATIONAL_NAME
    * @return The appropriate string (or null if not defined)
    */
   final public String getCertificateDN(int component)
   {
      if (ssl != null)
      {
         SSLSession session = ((SSLSocket)ssl).getSession();
         try
         {
            java.security.cert.Certificate chain[] = session.getPeerCertificates();
            if (chain == null || chain.length == 0 || chain[0] == null)
               return null;

            X500Principal principal = null;

            switch (component)
            {
            // issuer X500
            case Constants.SSL_X509_CA_CERT_COMMON_NAME:
            case Constants.SSL_X509_CA_CERT_ORGANIZATION:
            case Constants.SSL_X509_CA_CERT_ORGANIZATIONAL_NAME:
               principal = ((java.security.cert.X509Certificate)chain[0]).getIssuerX500Principal(); 
               break;
            
            // subject X500
            case Constants.SSL_X509_CERT_COMMON_NAME:
            case Constants.SSL_X509_CERT_ORGANIZATION:
            case Constants.SSL_X509_CERT_ORGANIZATIONAL_NAME:
               principal = ((java.security.cert.X509Certificate)chain[0]).getSubjectX500Principal(); 
               break;
            }
            
            if (principal == null)
               return null;
            
            String x500 = principal.getName(X500Principal.RFC2253);
            if (x500 == null || x500.length() == 0)
               return null;
            
            int s;
            switch (component)
            {
            
            case Constants.SSL_X509_CA_CERT_COMMON_NAME:
            case Constants.SSL_X509_CERT_COMMON_NAME:
               s = x500.indexOf("CN=");
               if (s >= 0)
                  return x500.substring(s+3, x500.indexOf(',', s));
               break;
               
            case Constants.SSL_X509_CA_CERT_ORGANIZATION:
            case Constants.SSL_X509_CERT_ORGANIZATION:
               s = x500.indexOf("O=");
               if (s >= 0)
               {
                  int lc = x500.indexOf(',', s);
                  s += 2;
                  if (lc >= s)
                     return x500.substring(s, lc);
                  else
                     return x500.substring(s);
               }
               break;
               
            case Constants.SSL_X509_CA_CERT_ORGANIZATIONAL_NAME:
            case Constants.SSL_X509_CERT_ORGANIZATIONAL_NAME:
               s = x500.indexOf("OU=");
               if (s >= 0)
                  return x500.substring(s+3, x500.indexOf(',', s));
               break;
            }
         }
         catch (IOException e)
         {
            lastException = e;
            Launcher.print(Vm.getStackTrace(e));
         }
      }
      return null;
   }

   /**
    * Read the SSL data stream.
    * Use rh before doing any successive ssl calls.
    * @param rh [out] After a successful read, the decrypted data can be
    * retrieved with rh.getData(). It will be null otherwise.
    * @return The number of decrypted bytes:
    * - if > 0, then the handshaking is complete and we are returning the
    * number of decrypted bytes.
    * - SSL_OK if the handshaking stage is successful (but not yet complete).
    * - < 0 if an error.
    */
   final public int read(SSLReadHolder rh)
   {
      if (ssl != null)
      {
         SSLSocket sslSocket = (SSLSocket)ssl;
         try
         {
            sslSocket.setSoTimeout(socket.readTimeout);
         }
         catch (IOException ex)
         {
            Launcher.print(Vm.getStackTrace(ex));
         }
         
         try
         {
            InputStream is = sslSocket.getInputStream();
            int r = is.read(); // first, read one byte using the timeout
            if (r != -1)
            {
               int count = is.available();
               byte[] buf = rh.m_buf = new byte[count + 1];
               
               buf[0] = (byte)r;
               if (count > 0)
                  is.read(buf, 1, count);

               return count + 1;
            }
         }
         catch (IOException ex)
         {
            lastException = ex;
            Launcher.print(Vm.getStackTrace(ex));
         }
      }
      
      return -1;
   }

   /**
    * Write to the SSL data stream.
    * @param out_data [in] The data to be written
    * @return The number of bytes sent, or if < 0 if an error.
    */
   final public int write(byte[] out_data)
   {
      return write(out_data, out_data.length);
   }

   /**
    * Write to the SSL data stream.
    * @param out_data [in] The data to be written
    * @param out_len [in] The number of bytes to be written
    * @return The number of bytes sent, or if < 0 if an error.
    */
   final public int write(byte[] out_data, int out_len)
   {
      if (ssl != null)
      {
         try
         {
            ((SSLSocket)ssl).getOutputStream().write(out_data, 0, out_len);
            return out_len;
         }
         catch (IOException e)
         {
            lastException = e;
            Launcher.print(Vm.getStackTrace(e));
         }
      }
      return -1;
   }

   /**
    * Authenticate a received certificate.
    * This call is usually made by a client after a handshake is complete
    * and the context is in SSL_SERVER_VERIFY_LATER mode.
    * @return SSL_OK if the certificate is verified.
    */
   final public int verifyCertificate()
   {
      for (int i = 0; i < _trustMgrs.length; i++)
      {
         try
         {
            Certificate[] certs = ((SSLSocket)ssl).getSession().getPeerCertificates();
            java.security.cert.X509Certificate[] _certs = new java.security.cert.X509Certificate[certs.length]; 
            for (int c = 0; c < certs.length; c++)
               _certs[c] = (java.security.cert.X509Certificate)certs[c];

            javax.net.ssl.X509TrustManager x509_tm = (javax.net.ssl.X509TrustManager)_trustMgrs[i];
            x509_tm.checkServerTrusted(_certs, "RSA");

            // found one trust manager that could verify the peer certificate chain
            return Constants.SSL_OK;
         }
         catch (SSLPeerUnverifiedException ex) { /* no remote certificate */ }
         catch (ValidatorException ex)         { /* remote certificate is not trusted! */ }
         catch (CertificateException ex)
         {
            lastException = ex;
            Launcher.print(Vm.getStackTrace(ex));
         }
      }
      return Constants.X509_VFY_ERROR_NO_TRUSTED_CERT;
   }

   /**
    * Force the client to perform its handshake again.
    * For a client this involves sending another "client hello" message.
    * For the server is means sending a "hello request" message.
    * This is a blocking call on the client (until the handshake completes).
    * @return SSL_OK if renegotiation instantiation was ok
    */
   final public int renegotiate()
   {
      if (ssl != null)
      {
         status = Constants.SSL_HANDSHAKE_IN_PROGRESS;
         try
         {
            ((SSLSocket)ssl).startHandshake();
         }
         catch (Exception e)
         {
            lastException = e;
            Launcher.print(Vm.getStackTrace(e));
         }
      }
      return Constants.SSL_OK;
   }
   
   /**
    * @return the last exception occurred in this SSL connection.
    * @since TotalCross 1.20
    */
   final public Exception getLastException()
   {
      return lastException;
   }
}
