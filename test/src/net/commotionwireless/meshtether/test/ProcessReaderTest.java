package net.commotionwireless.meshtether.test;

import java.io.StringBufferInputStream;

import net.commotionwireless.meshtether.MeshTetherProcess;
import android.test.AndroidTestCase;
import android.util.Log;

public class ProcessReaderTest extends AndroidTestCase {

	private MeshTetherProcess.ProcessReader mPr;
	public void setUp() {

	}
	
	public void tearDown() {
		
	}
	
	public void testLineExactLengthOfBuffer() {
		String line = null;
		String testLine = "line!\n";
		StringBufferInputStream is = new StringBufferInputStream(testLine);
		mPr = new MeshTetherProcess.ProcessReader(is, 6);
		line = mPr.readLine();
		
		assertEquals("Lines are not equal", testLine, line);
	}
	
	public void testNoEndingNewline() {
		String line = null;
		String testLine = "this is a line!";
		StringBufferInputStream is = new StringBufferInputStream(testLine);
		mPr = new MeshTetherProcess.ProcessReader(is, 4);
		line = mPr.readLine();
		
		assertEquals("Lines are not equal", testLine, line);
	}
	
	public void testLineLongerThanSingleBuffer() {
		String line = null;
		String testLine = "this is a line!\n";
		StringBufferInputStream is = new StringBufferInputStream(testLine);
		mPr = new MeshTetherProcess.ProcessReader(is, 4);
		line = mPr.readLine();
		
		assertEquals("Lines are not equal", testLine, line);
	}
	
	public void testReadFourLinesInOneBuffer() {
		StringBufferInputStream is = new StringBufferInputStream("this\nis\na\ntest!\n");
		mPr = new MeshTetherProcess.ProcessReader(is);
		String line = null;
		int linesRead = 0;
		do {
			line = mPr.readLine();
			Log.d("ProcessReaderTest", "line: -" + line + "-");
			linesRead++;
		} while (line != null);
		assertEquals("Did not read proper number of lines.", 4, linesRead-1);
	}
}
