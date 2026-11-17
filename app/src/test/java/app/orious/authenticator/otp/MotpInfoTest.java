package app.orious.authenticator.otp;

import static org.junit.Assert.assertEquals;

import app.orious.authenticator.crypto.otp.MOTPTest;
import app.orious.authenticator.encoding.EncodingException;
import app.orious.authenticator.encoding.Hex;

import org.junit.Test;

public class MotpInfoTest {
    @Test
    public void testMotpInfoOtp() throws OtpInfoException, EncodingException {
        for (MOTPTest.Vector vector : MOTPTest.VECTORS) {
            MotpInfo info = new MotpInfo(Hex.decode(vector.Secret), vector.Pin);
            assertEquals(vector.OTP, info.getOtp(vector.Time));
        }
    }
}
