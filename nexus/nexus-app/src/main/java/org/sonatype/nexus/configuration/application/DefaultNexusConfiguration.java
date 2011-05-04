/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://www.sonatype.com/products/nexus/attributions.
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.configuration.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.shiro.subject.Subject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.NexusStreamResponse;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.ConfigurationCommitEvent;
import org.sonatype.nexus.configuration.ConfigurationLoadEvent;
import org.sonatype.nexus.configuration.ConfigurationPrepareForLoadEvent;
import org.sonatype.nexus.configuration.ConfigurationPrepareForSaveEvent;
import org.sonatype.nexus.configuration.ConfigurationRollbackEvent;
import org.sonatype.nexus.configuration.ConfigurationSaveEvent;
import org.sonatype.nexus.configuration.application.runtime.ApplicationRuntimeConfigurationBuilder;
import org.sonatype.nexus.configuration.model.CPathMappingItem;
import org.sonatype.nexus.configuration.model.CRemoteNexusInstance;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.source.ApplicationConfigurationSource;
import org.sonatype.nexus.configuration.validator.ApplicationConfigurationValidator;
import org.sonatype.nexus.configuration.validator.ApplicationValidationContext;
import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.events.VetoFormatter;
import org.sonatype.nexus.proxy.events.VetoFormatterRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.storage.local.DefaultLocalStorageContext;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.security.SecuritySystem;

/**
 * The class DefaultNexusConfiguration is responsible for config management. It actually keeps in sync Nexus internal
 * state with p ersisted user configuration. All changes incoming thru its iface is reflect/maintained in Nexus current
 * state and Nexus user config.
 * 
 * @author cstamas
 */
@Component( role = NexusConfiguration.class )
public class DefaultNexusConfiguration
    implements NexusConfiguration
{
    @Requirement
    private Logger logger;

    @Requirement
    private ApplicationEventMulticaster applicationEventMulticaster;

    @Requirement( hint = "file" )
    private ApplicationConfigurationSource configurationSource;

    /** The global local storage context. */
    private LocalStorageContext globalLocalStorageContext;

    /** The global remote storage context. */
    private RemoteStorageContext globalRemoteStorageContext;

    @Requirement
    private GlobalRemoteConnectionSettings globalRemoteConnectionSettings;

    @Requirement
    private GlobalHttpProxySettings globalHttpProxySettings;

    /**
     * The config validator.
     */
    @Requirement
    private ApplicationConfigurationValidator configurationValidator;

    /**
     * The runtime configuration builder.
     */
    @Requirement
    private ApplicationRuntimeConfigurationBuilder runtimeConfigurationBuilder;

    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement( role = ScheduledTaskDescriptor.class )
    private List<ScheduledTaskDescriptor> scheduledTaskDescriptors;

    @Requirement
    private SecuritySystem securitySystem;

    @org.codehaus.plexus.component.annotations.Configuration( value = "${nexus-work}" )
    private File workingDirectory;

    @Requirement
    private VetoFormatter vetoFormatter;

    /** The config dir */
    private File configurationDirectory;

    /** The temp dir */
    private File temporaryDirectory;

    /** Names of the conf files */
    private Map<String, String> configurationFiles;

    /** The default maxInstance count */
    private int defaultRepositoryMaxInstanceCountLimit = Integer.MAX_VALUE;

    /** The map with per-repotype limitations */
    private Map<RepositoryTypeDescriptor, Integer> repositoryMaxInstanceCountLimits;

    // ==

    protected Logger getLogger()
    {
        return logger;
    }

    // ==

    public void loadConfiguration()
        throws ConfigurationException, IOException
    {
        loadConfiguration( false );
    }

    public void loadConfiguration( boolean force )
        throws ConfigurationException, IOException
    {
        if ( force || configurationSource.getConfiguration() == null )
        {
            getLogger().info( "Loading Nexus Configuration..." );

            configurationSource.loadConfiguration();

            configurationDirectory = null;

            temporaryDirectory = null;

            globalLocalStorageContext = new DefaultLocalStorageContext( null );

            // create global remote ctx
            // this one has no parent
            globalRemoteStorageContext = new DefaultRemoteStorageContext( null );

            globalRemoteConnectionSettings.configure( this );

            globalRemoteStorageContext.setRemoteConnectionSettings( globalRemoteConnectionSettings );

            globalHttpProxySettings.configure( this );

            globalRemoteStorageContext.setRemoteProxySettings( globalHttpProxySettings );

            ConfigurationPrepareForLoadEvent loadEvent = new ConfigurationPrepareForLoadEvent( this );

            applicationEventMulticaster.notifyEventListeners( loadEvent );

            if ( loadEvent.isVetoed() )
            {
                getLogger().info(
                    vetoFormatter.format( new VetoFormatterRequest( loadEvent, getLogger().isDebugEnabled() ) ) );

                throw new ConfigurationException( "The Nexus configuration is invalid!" );
            }

            applyConfiguration();

            // we succesfully loaded config
            applicationEventMulticaster.notifyEventListeners( new ConfigurationLoadEvent( this ) );
        }
    }

    public boolean applyConfiguration()
    {
        getLogger().info( "Applying Nexus Configuration..." );

        ConfigurationPrepareForSaveEvent prepare = new ConfigurationPrepareForSaveEvent( this );

        applicationEventMulticaster.notifyEventListeners( prepare );

        if ( !prepare.isVetoed() )
        {
            configurationDirectory = null;

            temporaryDirectory = null;

            applicationEventMulticaster.notifyEventListeners( new ConfigurationCommitEvent( this ) );

            String userId = null;
            Subject subject = securitySystem.getSubject();
            if ( subject != null && subject.getPrincipal() != null )
            {
                userId = subject.getPrincipal().toString();
            }

            applicationEventMulticaster.notifyEventListeners( new ConfigurationChangeEvent( this, prepare.getChanges(),
                userId ) );

            return true;
        }
        else
        {
            getLogger().info( vetoFormatter.format( new VetoFormatterRequest( prepare, getLogger().isDebugEnabled() ) ) );

            applicationEventMulticaster.notifyEventListeners( new ConfigurationRollbackEvent( this ) );

            return false;
        }
    }

    public synchronized void saveConfiguration()
        throws IOException
    {
        if ( applyConfiguration() )
        {
            // TODO: when NEXUS-2215 is fixed, this should be remove/moved/cleaned
            // START <<<
            // validate before we do anything
            ValidationRequest request = new ValidationRequest( configurationSource.getConfiguration() );
            ValidationResponse response = configurationValidator.validateModel( request );
            if ( !response.isValid() )
            {
                this.getLogger().error( "Saving nexus configuration caused unexpected error:\n" + response.toString() );
                throw new IOException( "Saving nexus configuration caused unexpected error:\n" + response.toString() );
            }
            // END <<<

            configurationSource.storeConfiguration();

            // we succesfully saved config
            applicationEventMulticaster.notifyEventListeners( new ConfigurationSaveEvent( this ) );
        }
    }

    @Deprecated
    // see above
    protected void applyAndSaveConfiguration()
        throws IOException
    {
        saveConfiguration();
    }

    @Deprecated
    public Configuration getConfigurationModel()
    {
        return configurationSource.getConfiguration();
    }

    public ApplicationConfigurationSource getConfigurationSource()
    {
        return configurationSource;
    }

    public boolean isInstanceUpgraded()
    {
        // TODO: this is not quite true: we might keep model ver but upgrade JARs of Nexus only in a release
        // we should store the nexus version somewhere in working storage and trigger some household stuff
        // if version changes.
        return configurationSource.isConfigurationUpgraded();
    }

    public boolean isConfigurationUpgraded()
    {
        return configurationSource.isConfigurationUpgraded();
    }

    public boolean isConfigurationDefaulted()
    {
        return configurationSource.isConfigurationDefaulted();
    }

    public LocalStorageContext getGlobalLocalStorageContext()
    {
        return globalLocalStorageContext;
    }

    public RemoteStorageContext getGlobalRemoteStorageContext()
    {
        return globalRemoteStorageContext;
    }

    public File getWorkingDirectory()
    {
        // Create the dir if doesn't exist, throw runtime exception on failure
        // bad bad bad
        if ( !workingDirectory.exists() && !workingDirectory.mkdirs() )
        {
            String message =
                "\r\n******************************************************************************\r\n"
                    + "* Could not create work directory [ " + workingDirectory.toString() + "]!!!! *\r\n"
                    + "* Nexus cannot start properly until the process has read+write permissions to this folder *\r\n"
                    + "******************************************************************************";

            getLogger().fatalError( message );
        }

        return workingDirectory;
    }

    public File getWorkingDirectory( String key )
    {
        File keyedDirectory = new File( getWorkingDirectory(), key );

        if ( !keyedDirectory.isDirectory() && !keyedDirectory.mkdirs() )
        {
            String message =
                "\r\n******************************************************************************\r\n"
                    + "* Could not create work directory [ " + keyedDirectory.toString() + "]!!!! *\r\n"
                    + "* Nexus cannot start properly until the process has read+write permissions to this folder *\r\n"
                    + "******************************************************************************";

            getLogger().fatalError( message );
        }

        return keyedDirectory;
    }

    public File getTemporaryDirectory()
    {
        if ( temporaryDirectory == null )
        {
            temporaryDirectory = new File( System.getProperty( "java.io.tmpdir" ) );

            if ( !temporaryDirectory.exists() )
            {
                temporaryDirectory.mkdirs();
            }
        }
        return temporaryDirectory;
    }

    public File getConfigurationDirectory()
    {
        if ( configurationDirectory == null )
        {
            configurationDirectory = new File( getWorkingDirectory(), "conf" );

            if ( !configurationDirectory.exists() )
            {
                configurationDirectory.mkdirs();
            }

        }
        return configurationDirectory;
    }

    @Deprecated
    public Repository createRepositoryFromModel( CRepository repository )
        throws ConfigurationException
    {
        return runtimeConfigurationBuilder.createRepositoryFromModel( getConfigurationModel(), repository );
    }

    public List<ScheduledTaskDescriptor> listScheduledTaskDescriptors()
    {
        return Collections.unmodifiableList( scheduledTaskDescriptors );
    }

    public ScheduledTaskDescriptor getScheduledTaskDescriptor( String id )
    {
        for ( ScheduledTaskDescriptor descriptor : scheduledTaskDescriptors )
        {
            if ( descriptor.getId().equals( id ) )
            {
                return descriptor;
            }
        }

        return null;
    }

    // ------------------------------------------------------------------
    // Security

    public boolean isSecurityEnabled()
    {
        return getSecuritySystem() != null && getSecuritySystem().isSecurityEnabled();
    }

    public void setSecurityEnabled( boolean enabled )
        throws IOException
    {
        getSecuritySystem().setSecurityEnabled( enabled );
    }

    public void setRealms( List<String> realms )
        throws org.sonatype.configuration.validation.InvalidConfigurationException
    {
        getSecuritySystem().setRealms( realms );
    }

    public boolean isAnonymousAccessEnabled()
    {
        return getSecuritySystem() != null && getSecuritySystem().isAnonymousAccessEnabled();
    }

    public void setAnonymousAccessEnabled( boolean enabled )
    {
        getSecuritySystem().setAnonymousAccessEnabled( enabled );
    }

    public String getAnonymousUsername()
    {
        return getSecuritySystem().getAnonymousUsername();
    }

    public void setAnonymousUsername( String val )
        throws org.sonatype.configuration.validation.InvalidConfigurationException
    {
        getSecuritySystem().setAnonymousUsername( val );
    }

    public String getAnonymousPassword()
    {
        return getSecuritySystem().getAnonymousPassword();
    }

    public void setAnonymousPassword( String val )
        throws org.sonatype.configuration.validation.InvalidConfigurationException
    {
        getSecuritySystem().setAnonymousPassword( val );
    }

    public List<String> getRealms()
    {
        return getSecuritySystem().getRealms();
    }

    // ------------------------------------------------------------------
    // Booting

    public void createInternals()
        throws ConfigurationException
    {
        createRepositories();
    }

    public void dropInternals()
    {
        dropRepositories();
    }

    protected void createRepositories()
        throws ConfigurationException
    {
        List<CRepository> reposes = getConfigurationModel().getRepositories();

        for ( CRepository repo : reposes )
        {

            if ( !repo.getProviderRole().equals( GroupRepository.class.getName() ) )
            {
                instantiateRepository( getConfigurationModel(), repo );
            }
        }

        for ( CRepository repo : reposes )
        {
            if ( repo.getProviderRole().equals( GroupRepository.class.getName() ) )
            {
                instantiateRepository( getConfigurationModel(), repo );
            }
        }
    }

    protected void dropRepositories()
    {
        for ( Repository repository : repositoryRegistry.getRepositories() )
        {
            try
            {
                repositoryRegistry.removeRepositorySilently( repository.getId() );
            }
            catch ( NoSuchRepositoryException e )
            {
                // will not happen
            }
        }
    }

    protected Repository instantiateRepository( Configuration configuration, CRepository repositoryModel )
        throws ConfigurationException
    {
        checkRepositoryMaxInstanceCountForCreation( repositoryModel );

        // create it, will do runtime validation
        Repository repository = runtimeConfigurationBuilder.createRepositoryFromModel( configuration, repositoryModel );

        // register with repoRegistry
        repositoryRegistry.addRepository( repository );

        // give it back
        return repository;
    }

    // ------------------------------------------------------------------
    // CRUD-like ops on config sections
    // Globals are mandatory: RU

    // CRepository and CreposioryShadow helper

    private ApplicationValidationContext getRepositoryValidationContext()
    {
        ApplicationValidationContext result = new ApplicationValidationContext();

        fillValidationContextRepositoryIds( result );

        return result;
    }

    private void fillValidationContextRepositoryIds( ApplicationValidationContext context )
    {
        context.addExistingRepositoryIds();

        List<CRepository> repositories = getConfigurationModel().getRepositories();

        if ( repositories != null )
        {
            for ( CRepository repo : repositories )
            {
                context.getExistingRepositoryIds().add( repo.getId() );
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------
    // Repositories
    // ----------------------------------------------------------------------------------------------------------

    protected Map<RepositoryTypeDescriptor, Integer> getRepositoryMaxInstanceCountLimits()
    {
        if ( repositoryMaxInstanceCountLimits == null )
        {
            repositoryMaxInstanceCountLimits = new ConcurrentHashMap<RepositoryTypeDescriptor, Integer>();
        }

        return repositoryMaxInstanceCountLimits;
    }

    public void setDefaultRepositoryMaxInstanceCount( int count )
    {
        if ( count < 0 )
        {
            getLogger().info( "Default repository maximal instance limit set to UNLIMITED." );

            this.defaultRepositoryMaxInstanceCountLimit = Integer.MAX_VALUE;
        }
        else
        {
            getLogger().info( "Default repository maximal instance limit set to " + count + "." );

            this.defaultRepositoryMaxInstanceCountLimit = count;
        }
    }

    public void setRepositoryMaxInstanceCount( RepositoryTypeDescriptor rtd, int count )
    {
        if ( count < 0 )
        {
            getLogger().info( "Repository type " + rtd.toString() + " maximal instance limit set to UNLIMITED." );

            getRepositoryMaxInstanceCountLimits().remove( rtd );
        }
        else
        {
            getLogger().info( "Repository type " + rtd.toString() + " maximal instance limit set to " + count + "." );

            getRepositoryMaxInstanceCountLimits().put( rtd, count );
        }
    }

    public int getRepositoryMaxInstanceCount( RepositoryTypeDescriptor rtd )
    {
        Integer limit = getRepositoryMaxInstanceCountLimits().get( rtd );

        if ( null != limit )
        {
            return limit;
        }
        else
        {
            return defaultRepositoryMaxInstanceCountLimit;
        }
    }

    protected void checkRepositoryMaxInstanceCountForCreation( CRepository repositoryModel )
        throws ConfigurationException
    {
        RepositoryTypeDescriptor rtd =
            repositoryTypeRegistry.getRepositoryTypeDescriptor( repositoryModel.getProviderRole(),
                repositoryModel.getProviderHint() );

        int maxCount;

        if ( null == rtd )
        {
            // rtd =
            // new RepositoryTypeDescriptor( repositoryModel.getProviderRole(), repositoryModel.getProviderHint(),
            // "repositories", RepositoryType.UNLIMITED_INSTANCES );
            //
            // getLogger().warn(
            // "Your Nexus instance contains a plugin, that contributes repository type " + rtd.toString()
            // + " to Nexus, "
            // + "but fails to properly register it (and it is not using Nexus Plugin API annotations either). "
            // + "Please contact plugin developers to update their plugin! Registering the type on-the-fly." );
            //
            // repositoryTypeRegistry.registerRepositoryTypeDescriptors( rtd );

            String msg =
                String.format(
                    "Repository \"%s\" (repoId=%s) cannot be created. It's repository type %s:%s is unknown to Nexus Core. It is probably contributed by an old Nexus plugin. Please contact plugin developers to upgrade the plugin, to register the new repository type properly!",
                    repositoryModel.getName(), repositoryModel.getId(), repositoryModel.getProviderRole(),
                    repositoryModel.getProviderHint() );

            getLogger().error( msg );

            throw new ConfigurationException( msg );
        }

        if ( rtd.getRepositoryMaxInstanceCount() != RepositoryType.UNLIMITED_INSTANCES )
        {
            maxCount = rtd.getRepositoryMaxInstanceCount();
        }
        else
        {
            maxCount = getRepositoryMaxInstanceCount( rtd );
        }

        if ( rtd.getInstanceCount() >= maxCount )
        {
            String msg =
                "Repository \"" + repositoryModel.getName() + "\" (id=" + repositoryModel.getId()
                    + ") cannot be created. It's repository type " + rtd.toString() + " is limited to " + maxCount
                    + " instances, and it already has " + String.valueOf( rtd.getInstanceCount() ) + " of them.";

            getLogger().warn( msg );

            throw new ConfigurationException( msg );
        }
    }

    // CRepository: CRUD

    protected void validateRepository( CRepository settings, boolean create )
        throws ConfigurationException
    {
        ApplicationValidationContext ctx = getRepositoryValidationContext();

        if ( !create && !StringUtils.isEmpty( settings.getId() ) )
        {
            // remove "itself" from the list to avoid hitting "duplicate repo" problem
            ctx.getExistingRepositoryIds().remove( settings.getId() );
        }

        ValidationResponse vr = configurationValidator.validateRepository( ctx, settings );

        if ( !vr.isValid() )
        {
            throw new InvalidConfigurationException( vr );
        }
    }

    public Repository createRepository( CRepository settings )
        throws ConfigurationException, IOException
    {
        validateRepository( settings, true );

        // create it, will do runtime validation
        Repository repository = instantiateRepository( getConfigurationModel(), settings );

        // now add it to config, since it is validated and succesfully created
        getConfigurationModel().addRepository( settings );

        // save
        saveConfiguration();

        return repository;
    }

    public void deleteRepository( String id )
        throws NoSuchRepositoryException, IOException, ConfigurationException
    {
        Repository repository = repositoryRegistry.getRepository( id );
        // put out of service so wont be accessed any longer
        repository.setLocalStatus( LocalStatus.OUT_OF_SERVICE );
        // disable indexing for same purpose
        repository.setIndexable( false );
        repository.setSearchable( false );

        // remove dependants too

        // =======
        // shadows
        // (fail if any repo references the currently processing one)
        List<ShadowRepository> shadows = repositoryRegistry.getRepositoriesWithFacet( ShadowRepository.class );

        for ( Iterator<ShadowRepository> i = shadows.iterator(); i.hasNext(); )
        {
            ShadowRepository shadow = i.next();

            if ( repository.getId().equals( shadow.getMasterRepository().getId() ) )
            {
                throw new ConfigurationException( "The repository with ID " + id
                    + " is not deletable, it has dependant repositories!" );
            }
        }

        // ======
        // groups
        // (correction in config only, registry DOES handle it)
        // since NEXUS-1770, groups are "self maintaining"

        // ===========
        // pahMappings
        // (correction, since registry is completely unaware of this component)

        List<CPathMappingItem> pathMappings = getConfigurationModel().getRepositoryGrouping().getPathMappings();

        for ( Iterator<CPathMappingItem> i = pathMappings.iterator(); i.hasNext(); )
        {
            CPathMappingItem item = i.next();

            item.removeRepository( id );
        }

        // ===========
        // and finally
        // this cleans it properly from the registry (from reposes and repo groups)
        repositoryRegistry.removeRepository( id );

        List<CRepository> reposes = getConfigurationModel().getRepositories();

        for ( Iterator<CRepository> i = reposes.iterator(); i.hasNext(); )
        {
            CRepository repo = i.next();

            if ( repo.getId().equals( id ) )
            {
                i.remove();

                applyAndSaveConfiguration();

                return;
            }
        }

        throw new NoSuchRepositoryException( id );
    }

    // ===

    public Collection<CRemoteNexusInstance> listRemoteNexusInstances()
    {
        List<CRemoteNexusInstance> result = null;

        if ( getConfigurationModel().getRemoteNexusInstances() != null )
        {
            result = Collections.unmodifiableList( getConfigurationModel().getRemoteNexusInstances() );
        }

        return result;
    }

    public CRemoteNexusInstance readRemoteNexusInstance( String alias )
        throws IOException
    {
        List<CRemoteNexusInstance> knownInstances = getConfigurationModel().getRemoteNexusInstances();

        for ( Iterator<CRemoteNexusInstance> i = knownInstances.iterator(); i.hasNext(); )
        {
            CRemoteNexusInstance nexusInstance = i.next();

            if ( nexusInstance.getAlias().equals( alias ) )
            {
                return nexusInstance;
            }
        }

        return null;
    }

    public void createRemoteNexusInstance( CRemoteNexusInstance settings )
        throws IOException
    {
        getConfigurationModel().addRemoteNexusInstance( settings );

        applyAndSaveConfiguration();
    }

    public void deleteRemoteNexusInstance( String alias )
        throws IOException
    {
        List<CRemoteNexusInstance> knownInstances = getConfigurationModel().getRemoteNexusInstances();

        for ( Iterator<CRemoteNexusInstance> i = knownInstances.iterator(); i.hasNext(); )
        {
            CRemoteNexusInstance nexusInstance = i.next();

            if ( nexusInstance.getAlias().equals( alias ) )
            {
                i.remove();
            }
        }

        applyAndSaveConfiguration();
    }

    public Map<String, String> getConfigurationFiles()
    {
        if ( configurationFiles == null )
        {
            configurationFiles = new HashMap<String, String>();

            File configDirectory = getConfigurationDirectory();

            int key = 1;

            // Tamas:
            // configDirectory.listFiles() may be returning null... in this case, it is 99.9% not true (otherwise nexus
            // would not start at all), but in general, be more explicit about checks.

            if ( configDirectory.isDirectory() && configDirectory.listFiles() != null )
            {
                for ( File file : configDirectory.listFiles() )
                {
                    if ( file.exists() && file.isFile() )
                    {
                        configurationFiles.put( Integer.toString( key ), file.getName() );

                        key++;
                    }
                }
            }
        }
        return configurationFiles;
    }

    public NexusStreamResponse getConfigurationAsStreamByKey( String key )
        throws IOException
    {
        String fileName = getConfigurationFiles().get( key );

        if ( fileName != null )
        {
            File configFile = new File( getConfigurationDirectory(), fileName );

            if ( configFile.canRead() && configFile.isFile() )
            {
                NexusStreamResponse response = new NexusStreamResponse();

                response.setName( fileName );

                if ( fileName.endsWith( ".xml" ) )
                {
                    response.setMimeType( "text/xml" );
                }
                else
                {
                    response.setMimeType( "text/plain" );
                }

                response.setSize( configFile.length() );
                response.setFromByte( 0 );
                response.setBytesCount( configFile.length() );
                response.setInputStream( new FileInputStream( configFile ) );

                return response;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    protected SecuritySystem getSecuritySystem()
    {
        return this.securitySystem;
    }
}
