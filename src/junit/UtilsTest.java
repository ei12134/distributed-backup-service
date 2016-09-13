package junit;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;

import utils.Utils;

public class UtilsTest {

	@Test
	public void testSha256Sum() throws IOException {
		assertEquals(
				"46070d4bf934fb0d4b06d9e2c46e346944e322444900a435d7d9a95e6d7435f5",
				Utils.sha256Sum("teste"));
		assertEquals(
				"b2c1bb6252c355108a32a6561a7e39db6c2cc34326eb8d9c9a6cf7f44bf59ae0",
				Utils.sha256Sum("aspokdfgnp+asidngpásiodngpasid_4123580hsdng"));
		assertEquals(
				"6a0863bbbfd4bc674656c53f1a47ffc50bfa85043e1a52f69c4d975dee017577",
				Utils.sha256Sum("apsdmfasºçd.º+bpvnk'w495jy2«4n51246"));
	}
}
