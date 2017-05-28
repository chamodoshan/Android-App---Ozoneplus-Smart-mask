package com.sk.ozoneplus;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ShimaK on 28-May-17.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GasActivityTest {
    GasActivity gasActivity;

    @Before
    public void initialize() {
        gasActivity = new GasActivity();
    }

    @Test
    public void test_a_labelFormer() {
        assertThat(gasActivity.labelFormer(59), is(equalTo(59d)));
    }

    @Test
    public void test_b_labelFormer() {
        assertThat(gasActivity.labelFormer(60), is(equalTo(0d)));
    }

    @Test
    public void test_c_labelFormer() {
        assertThat(gasActivity.labelFormer(61), is(equalTo(1d)));
    }

    @Test
    public void test_d_labelFormer() {
        assertThat(gasActivity.labelFormer(62), is(equalTo(2d)));
    }
}
