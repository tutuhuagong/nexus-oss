package org.sonatype.security.realms.tools;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.context.Context;
import org.slf4j.Logger;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;

public class ResourceMergingManagerThreadedTest
    extends PlexusTestCase
{
    private ConfigurationManager manager;

    private List<UnitTestSecurityResource> securityResources = new ArrayList<UnitTestSecurityResource>();
    
    private int expectedPrivilegeCount = 0;

    @Override
    protected void customizeContainerConfiguration( ContainerConfiguration configuration )
    {
        configuration.setClassPathScanning( true );
    }
    
    @Override
    protected void customizeContext( Context context )
    {
        super.customizeContext( context );

        context.put(
            "security-xml-file",
            "target/test-classes/org/sonatype/jsecurity/configuration/static-merging/security.xml" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        this.manager = (ConfigurationManager) lookup( ConfigurationManager.class, "resourceMerging" );
        
        for ( int ii = 1; ii < 100; ii++ )
        {
            UnitTestSecurityResource resource = (UnitTestSecurityResource) this.lookup( StaticSecurityResource.class, "UnitTestSecurityResource" );
            this.securityResources.add( resource );
            
            ComponentDescriptor templateDescriptor = this.getContainer().getComponentDescriptor( StaticSecurityResource.class.getName(), "UnitTestSecurityResource" );
            
            // now add it to the container
            this.getContainer().addComponent( resource, StaticSecurityResource.class, "default" );
            
            ComponentDescriptor<StaticSecurityResource> descriptor = new ComponentDescriptor<StaticSecurityResource>();
            descriptor.setRole( StaticSecurityResource.class.getName() );
            descriptor.setRoleHint( "test-"+ii );
            descriptor.setImplementationClass( UnitTestSecurityResource.class);
//            descriptor.addRequirement( (ComponentRequirement) templateDescriptor.getRequirements().get( 0 ) );
            
            this.getContainer().addComponentDescriptor( descriptor );
        }
        
        this.expectedPrivilegeCount = this.manager.listPrivileges().size();
    }

    public void testThreading()
        throws Throwable
    {
        TestFramework.runOnce( new MultithreadedTestCase()
        {
            public void initialize()
            {
                securityResources.get( 0 ).dirty = true;
            }

            public void thread1()
            {
                Assert.assertEquals( expectedPrivilegeCount, manager.listPrivileges().size() );
            }
            
            public void thread2()
            {
                Assert.assertEquals( expectedPrivilegeCount, manager.listPrivileges().size() );
            }

            public void thread3()
            {
                Assert.assertEquals( expectedPrivilegeCount, manager.listPrivileges().size() );
            }
            
            public void thread4()
            {
                Assert.assertEquals( expectedPrivilegeCount, manager.listPrivileges().size() );
            }
            
            public void thread5()
            {
                Assert.assertEquals( expectedPrivilegeCount, manager.listPrivileges().size() );
            }

        });//, Integer.MAX_VALUE, Integer.MAX_VALUE ); // uncomment this for debugging, if you don't the framework will timeout and close your debug session

    }

}
