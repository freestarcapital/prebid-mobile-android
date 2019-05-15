package org.prebid.mobile;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FreestarDirectHandlerTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void holder() {
    }

    @Test
    public void populatePost() throws JSONException {
        File f = new File("src/test/java/org/prebid/mobile/Freestar.txt");
        System.out.println(f.getAbsoluteFile());
        System.out.println(f.exists());
        StringBuilder json = new StringBuilder();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            while((line = br.readLine()) != null) {
                json.append(line);
                json.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject result = new JSONObject(json.toString());
        String auctionId="111";
        Map<String, String> keywords = new HashMap();

        boolean b = FreestarDirectHandler.process(result, auctionId, keywords);
        assertTrue(b);

    }
}