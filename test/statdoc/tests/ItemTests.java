package statdoc.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import statdoc.items.Item;
import statdoc.items.MatchItem;

@RunWith(JUnit4.class)
public class ItemTests {

    @Test
    public void testMatchedFirst() {
        
        Item item = new Item( "test", "test", "test" );
        item.put( "test", "test test2 test3*" );
        
        MatchItem match = new MatchItem( "match", "test:" );
        match.put("term","test");
        match.put("field", "test");
        item.addChild(match);
        
        Item resolve = new Item( "resolve","resolve","resolveFirstOnly" ); 
        match.addChild(resolve);
        
        String matched = item.getMatched("test");
        System.out.println(matched);
        org.junit.Assert.assertEquals( matched.length(), 51 );
        org.junit.Assert.assertTrue( matched.contains("../resolveFirstOnly") );
    }

    @Test
    public void testMatchedMiddle() {
        
        Item item = new Item( "test", "test", "test" );
        item.put( "test", "test1 test2 test3*" );
        
        MatchItem match = new MatchItem( "match", "test:" );
        match.put("term","test2");
        match.put("field", "test");
        item.addChild(match);
        
        Item resolve = new Item( "resolve","resolve","resolveMiddle" ); 
        match.addChild(resolve);
        
        String matched = item.getMatched("test");
        System.out.println(matched);
        org.junit.Assert.assertEquals( matched.length(), 49 );
        org.junit.Assert.assertTrue( matched.contains("../resolveMiddle") );
    }


    @Test
    public void testMatchedMiddleStar() {
        
        Item item = new Item( "test", "test", "test" );
        item.put( "test", "test1 test2* test3*" );
        
        MatchItem match = new MatchItem( "match", "test:" );
        match.put("term","test2*");
        match.put("field", "test");
        item.addChild(match);
        
        Item resolve = new Item( "resolve","resolve","resolveMiddle" ); 
        match.addChild(resolve);
        
        String matched = item.getMatched("test");
        System.out.println(matched);
        org.junit.Assert.assertEquals( matched.length(), 50 );
        org.junit.Assert.assertTrue( matched.contains("../resolveMiddle") );
    }
    @Test
    public void testMatchedLastStar() {
        
        Item item = new Item( "test", "test", "test" );
        item.put( "test", "test1 test2 test3*" );
        
        MatchItem match = new MatchItem( "match", "test:" );
        match.put("term","test3*");
        match.put("field", "test");
        item.addChild(match);
        
        Item resolve = new Item( "resolve","resolve","resolveLast" ); 
        match.addChild(resolve);
        
        String matched = item.getMatched("test");
        System.out.println(matched);
        org.junit.Assert.assertEquals( matched.length(), 47 );
        org.junit.Assert.assertTrue( matched.contains("../resolveLast") );
    }

    @Test
    public void testMatchedFunction() {
        
        Item item = new Item( "test", "test", "test" );
        item.put( "test", "test1 function(test2) test3*" );
        
        MatchItem match = new MatchItem( "match", "test:" );
        match.put("term","function(");
        match.put("field", "test");
        item.addChild(match);
        
        Item resolve = new Item( "resolve","resolve","resolveFunction" ); 
        match.addChild(resolve);
        
        String matched = item.getMatched("test");
        System.out.println(matched);
        org.junit.Assert.assertEquals( matched.length(), 61 );
        org.junit.Assert.assertTrue( matched.contains("../resolveFunction") );
    }

    @Test
    public void testMatchedFunctionMulti() {
        
        Item item = new Item( "test", "test", "test" );
        item.put( "test", "double `sigma' = exp(`lnsigma')" );
        
        {
        MatchItem match = new MatchItem( "match", "test:" );
        match.put("term","exp(");
        match.put("field", "test");
        item.addChild(match);
        
        Item resolve = new Item( "resolve","resolveA","resolveExpFunction" ); 
        match.addChild(resolve);
        }
        
        {
        MatchItem match = new MatchItem( "match", "test:" );
        match.put("term","`lnsigma'");
        match.put("field", "test");
        item.addChild(match);
        
        Item resolve = new Item( "resolve","resolveB","resolveInFunction" ); 
        match.addChild(resolve);
        }
        
        String matched = item.getMatched("test");
        System.out.println(matched);
        org.junit.Assert.assertEquals( matched.length(), 102 );
        org.junit.Assert.assertTrue( matched.contains("../resolveExpFunction") );
        org.junit.Assert.assertTrue( matched.contains("../resolveInFunction") );
    }    
    


    @Test
    public void testMatchedStataLocal() {
        
        Item item = new Item( "test", "test", "test" );
        item.put( "test", "test1 test`x' test`x'_`y' test3*" );
        
        MatchItem match = new MatchItem( "match", "test:" );
        match.put("term","test`x'");
        match.put("field", "test");
        item.addChild(match);
        
        Item resolve = new Item( "resolve","resolve","resolveLocal" ); 
        match.addChild(resolve);
        
        String matched = item.getMatched("test");
        System.out.println(matched);
        org.junit.Assert.assertEquals( matched.length(), 62 );
        org.junit.Assert.assertTrue( matched.contains("../resolveLocal") );
    }

    @Test
    public void testMatchedStataLocalDouble() {
        
        Item item = new Item( "test", "test", "test" );
        item.put( "test", "ahalpha`x' = ahalpha`x' + ahalpha`x'_`y'" );
        
        {
        MatchItem match = new MatchItem( "match1", "test:" );
        match.put("term","ahalpha`x'");
        match.put("field", "test");
        item.addChild(match);
        
        {
            Item resolve = new Item( "resolve","resolveA","resolveLocalD1" ); 
            match.addChild(resolve);
        }
        {
            Item resolve = new Item( "resolve","resolveB","resolveLocalD2" ); 
            match.addChild(resolve);
        }
        }
        
        {
            MatchItem match = new MatchItem( "match0", "test:" );
            match.put("term","ahalpha`x'_`y'");
            match.put("field", "test");
            item.addChild(match);
            
            {
                Item resolve = new Item( "resolve","resolve","resolveLocalD3" ); 
                match.addChild(resolve);
            }
            {
                Item resolve = new Item( "resolve","resolve","resolveLocalD4" ); 
                match.addChild(resolve);
            }
            }
        
        String matched = item.getMatched("test");
        System.out.println( matched );
        org.junit.Assert.assertEquals(  190, matched.length() );
        org.junit.Assert.assertTrue( matched.contains("../resolveLocalD") );
    }
    
    
    @Test
    public void testMatchedOneOfMany() {

        String[][] pairs = new String[][] {
                new String[] { "b5*", "ahprob price maxgain maxprob gender age b5* ahnum ahset"},
                new String[] { "a", "a"},
                new String[] { "a", "a"},
                new String[] { "a", "a"},
                new String[] { "a", "a"},
                new String[] { "a", "a"},
        };
        
        for( String[] pair : pairs ) {
            Item item = new Item( "test", "test", "test" );
            item.put( "test", pair[1] );
            
            MatchItem match = new MatchItem( "match", "test:" );
            match.put("term", pair[0] );
            match.put("field", "test");
            item.addChild(match);
            
            Item resolve = new Item( "resolve","resolve","resolve" ); 
            match.addChild(resolve);
            
            String matched = item.getMatched("test");
            System.out.println(matched);
            org.junit.Assert.assertEquals( matched.length(), pair[1].length()+25 );
            org.junit.Assert.assertTrue( matched.contains("../resolve") );
        }
    }

}
