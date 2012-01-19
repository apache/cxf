package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.policy.PolicyCalculator;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Test;

public class PolicyDataEngineImplTest extends Assert {
    private final static QName TEST_POLICY_NAME = new QName("http://test","TestPolicy");
    
    class TestPolicy {
    }
    
    class TestPolicyCalculator implements PolicyCalculator<TestPolicy> {

        public Class<TestPolicy> getDataClass() {
            return TestPolicy.class;
        }

        public QName getDataClassName() {
            return TEST_POLICY_NAME;
        }

        public TestPolicy intersect(TestPolicy policy1, TestPolicy policy2) {
            return policy1;
        }

        public boolean isAsserted(Message message, TestPolicy policy, TestPolicy refPolicy) {
            return true;
        }
        
    }
    
    public AssertionInfo getTestPolicyAssertionInfo(TestPolicy policy) {
        JaxbAssertion<TestPolicy> assertion = 
            new JaxbAssertion<TestPolicy>(TEST_POLICY_NAME, false);
        assertion.setData(policy);
        return new AssertionInfo(assertion);
    }
    
    @Test
    public void testAssertMessageNullAim() {
        checkAssertWithMap(null);
    }
    
    @Test
    public void testAssertMessageEmptyAim() {
        checkAssertWithMap(new AssertionInfoMap(CastUtils.cast(Collections.EMPTY_LIST,
                                                               PolicyAssertion.class)));
    }

    @Test
    public void testAssertMessage() {
        TestPolicy policy = new TestPolicy();
        AssertionInfo ai = getTestPolicyAssertionInfo(policy);
        AssertionInfoMap aim = new AssertionInfoMap(CastUtils.cast(Collections.EMPTY_LIST,
                                                                   PolicyAssertion.class));
        Collection<AssertionInfo> ais = new ArrayList<AssertionInfo>();
        ais.add(ai);
        aim.put(TEST_POLICY_NAME, ais);
        checkAssertWithMap(aim);
        assertTrue(ai.isAsserted());
    }

    /**
     * Simply check that it runs without any exceptions
     * 
     * @param assertionInfoMap
     */
    private void checkAssertWithMap(AssertionInfoMap assertionInfoMap) {
        PolicyDataEngineImpl pde = new PolicyDataEngineImpl(null);
        pde.setPolicyEngine(new PolicyEngineImpl());
        TestPolicy confPol = new TestPolicy();
        IMocksControl control = EasyMock.createControl();
        PolicyCalculator<TestPolicy> policyCalculator = new TestPolicyCalculator();
        Message message = control.createMock(Message.class);
        EasyMock.expect(message.get(TestPolicy.class)).andReturn(confPol);
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(assertionInfoMap);
        control.replay();
        pde.assertMessage(message , confPol , policyCalculator);
        control.verify();
    }
}
