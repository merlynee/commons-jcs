package org.apache.jcs.auxiliary.remote;

import java.io.IOException;
import java.io.Serializable;

import java.util.HashMap;

import org.apache.jcs.access.exception.ObjectNotFoundException;

import org.apache.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.jcs.auxiliary.remote.behavior.IRemoteCacheService;

import org.apache.jcs.engine.behavior.IElementAttributes;
import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.CacheConstants;

import org.apache.jcs.engine.behavior.ICache;
import org.apache.jcs.engine.behavior.ICacheElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jcs.engine.behavior.IZombie;

/**
 * Client proxy for an RMI remote cache.
 *
 * @author asmuts
 * @created January 15, 2002
 */
public class RemoteCache implements ICache
{
    private final static Log log =
        LogFactory.getLog( RemoteCache.class );

    private static int numCreated = 0;

    final String cacheName;
    private IRemoteCacheService remote;
    private IRemoteCacheAttributes irca;

    IElementAttributes attr = null;

    private HashMap keyHash;
    // not synchronized to maximize concurrency.


    /** Description of the Method */
    public String toString()
    {
        return "RemoteCache: " + cacheName;
    }


    // was public but need to access from server
    /**
     * Constructor for the RemoteCache object
     *
     * @param cattr
     * @param remote
     */
    public RemoteCache( IRemoteCacheAttributes cattr, IRemoteCacheService remote )
    {
        this.irca = cattr;
        this.cacheName = cattr.getCacheName();
        this.remote = remote;

        if ( log.isDebugEnabled() )
        {
            log.debug( "Construct> cacheName=" + cattr.getCacheName() );
            log.debug( "irca = " + irca.toString() );
        }
        /*
         * TODO
         * should be done by the remote cache, not the job of the hub manager
         * Set up the idle period for the RemoteCacheMonitor.
         * long monPeriod = 0;
         * try {
         * monPeriod = Long.parseLong(props.getProperty("remote.monitor.idle.period", "0"));
         * } catch(NumberFormatException ex) {
         * log.warn(ex.getMessage());
         * }
         * RemoteCacheMonitor.setIdlePeriod(monPeriod);
         */
    }


    /**
     * Sets the attributes attribute of the RemoteCache object
     *
     * @param attr The new attributes value
     */
    public void setElementAttributes( IElementAttributes attr )
    {
        this.attr = attr;
    }


    /**
     * Gets the attributes attribute of the RemoteCache object
     *
     * @return The attributes value
     */
    public IElementAttributes getElementAttributes()
    {
        return this.attr;
    }


    /**
     * Synchronously put to the remote cache; if failed, replace the remote
     * handle with a zombie.
     */
    public void put( Serializable key, Serializable value )
        throws IOException
    {
        put( key, value, this.attr.copy() );
    }


    /** Description of the Method */
    public void put( Serializable key, Serializable value, IElementAttributes attr )
        throws IOException
    {
        try
        {
            CacheElement ce = new CacheElement( cacheName, sanitized( key ), sanitized( value ) );
            ce.setElementAttributes( attr );
            update( ce );
        }
        catch ( Exception ex )
        {
            handleException( ex, "Failed to put " + key + " to " + cacheName );
            //throw ex;
        }
    }


    /** Description of the Method */
    public void update( ICacheElement ce )
        throws IOException
    {
        // Do not communicate with cluster except via server.
        // separates the remote from the local.  Must run a server to
        // cluster, else it can be run inside a local.
        //if ( this.irca.getRemoteType() != irca.CLUSTER )
        if ( true )
        {

            if ( !this.irca.getGetOnly() )
            {
                try
                {
                    remote.update( ce, RemoteCacheInfo.listenerId );
                }
                catch ( NullPointerException npe )
                {
                    log.error( "npe for ce = " + ce + "ce.attr = " + ce.getElementAttributes(), npe );
                    return;
                }
                catch ( Exception ex )
                {
                    handleException( ex, "Failed to put " + ce.getKey() + " to " + ce.getCacheName() );
                    //throw ex;
                }
            }
            else
            {
                //p( "get only mode, irca = " + irca.toString() );
            }
        }
    }


    /**
     * Synchronously get from the remote cache; if failed, replace the remote
     * handle with a zombie.
     */
    public Serializable get( Serializable key )
        throws IOException
    {
        try
        {
            return remote.get( cacheName, sanitized( key ) );
        }
        catch ( ObjectNotFoundException one )
        {
            log.debug( "didn't find element " + key + " in remote" );
            return null;
        }
        catch ( Exception ex )
        {
            handleException( ex, "Failed to get " + key + " from " + cacheName );
            //throw ex;
            return null;
            // never executes; just keep the compiler happy.
        }
    }


    /**
     * Wraps a non JDK object into a MarshalledObject, so that we can avoid
     * unmarshalling the real object on the remote side. This technique offers
     * the benefit of surviving incompatible class versions without the need to
     * restart the remote cache server.
     */
    private Serializable sanitized( Serializable s )
        throws IOException
    {
        // In the unlikely case when the passed in object is a MarshalledObjct, we again wrap
        // it into a new MarsahlledObject for "escape" purposes during the get operation.
        //return s.getClass().getName().startsWith("java.") && !(s instanceof MarshalledObject) ? s : new MarshalledObject(s);

        // avoid this step for now, [problem with group id wrapper]
        return s;
    }


    /**
     * Synchronously get from the remote cache; if failed, replace the remote
     * handle with a zombie.
     */
    public Serializable get( Serializable key, boolean container )
        throws IOException
    {
        // TODO: rethink this with gets
        // Do not communicate with cluster except via server.
        // separates the remote from the local.  Must run a server to
        // cluster, else it can be run inside a local.
        //if ( this.irca.getRemoteType() != irca.CLUSTER )
        if ( true )
        {

            try
            {
                return remote.get( cacheName, sanitized( key ), container );
            }
            catch ( ObjectNotFoundException one )
            {
                log.debug( "didn't find element " + key + " in remote" );
                return null;
            }
            catch ( Exception ex )
            {
                handleException( ex, "Failed to get " + key + " from " + cacheName );
                return null;
                // never executes; just keep the compiler happy.
                //throw ex;
            }
        }
        return null;
    }


    /**
     * Synchronously remove from the remote cache; if failed, replace the remote
     * handle with a zombie.
     */
    public boolean remove( Serializable key )
        throws IOException
    {

        // Do not communicate with cluster except via server.
        // separates the remote from the local.  Must run a server to
        // cluster, else it can be run inside a local.
        //if ( this.irca.getRemoteType() != irca.CLUSTER )
        if ( true )
        {

            if ( !this.irca.getGetOnly() )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "remove> key=" + key );
                }
                try
                {
                    remote.remove( cacheName, sanitized( key ), RemoteCacheInfo.listenerId );
                }
                catch ( Exception ex )
                {
                    handleException( ex, "Failed to remove " + key + " from " + cacheName );
                    //throw ex;
                }
            }
        }
        return false;
    }


    /**
     * Synchronously removeAll from the remote cache; if failed, replace the
     * remote handle with a zombie.
     */
    public void removeAll()
        throws IOException
    {

        // Do not communicate with cluster except via server.
        // separates the remote from the local.  Must run a server to
        // cluster, else it can be run inside a local.
        if ( this.irca.getRemoteType() != irca.CLUSTER )
        {

            if ( !this.irca.getGetOnly() )
            {
                try
                {
                    remote.removeAll( cacheName, RemoteCacheInfo.listenerId );
                }
                catch ( Exception ex )
                {
                    handleException( ex, "Failed to remove all from " + cacheName );
                    //throw ex;
                }
            }
        }
    }


    /**
     * Synchronously dispose the remote cache; if failed, replace the remote
     * handle with a zombie.
     */
    public void dispose()
        throws IOException
    {
//    remote.freeCache(cacheName);
        log.debug( "disposing of remote cache" );
        try
        {
            remote.dispose( cacheName );
        }
        catch ( Exception ex )
        {
            log.error( "couldn't dispose" );
            handleException( ex, "Failed to dispose " + cacheName );
            //remote = null;
        }
    }


    /**
     * Gets the stats attribute of the RemoteCache object
     *
     * @return The stats value
     */
    public String getStats()
    {
        return "cacheName = " + cacheName;
    }


    /**
     * Returns the cache status. An error status indicates the remote connection
     * is not available.
     *
     * @return The status value
     */
    public int getStatus()
    {
        return remote instanceof IZombie ? CacheConstants.STATUS_ERROR : CacheConstants.STATUS_ALIVE;
    }


    /**
     * Returns the current cache size.
     *
     * @return The size value
     */
    public int getSize()
    {
        return 0;
    }


    /**
     * Gets the cacheType attribute of the RemoteCache object
     *
     * @return The cacheType value
     */
    public int getCacheType()
    {
        return REMOTE_CACHE;
    }


    /**
     * Gets the cacheName attribute of the RemoteCache object
     *
     * @return The cacheName value
     */
    public String getCacheName()
    {
        return cacheName;
    }


    /**
     * Replaces the current remote cache service handle with the given handle.
     */
    public void fixCache( IRemoteCacheService remote )
    {
        this.remote = remote;
        return;
    }


    /**
     * Handles exception by disabling the remote cache service before
     * re-throwing the exception in the form of an IOException.
     */
    private void handleException( Exception ex, String msg )
        throws IOException
    {
        log.error( "Disabling remote cache due to error " + msg );
        //log.error(ex);
        log.error( ex.toString() );
        remote = new ZombieRemoteCacheService();
        // may want to flush if region specifies
        // Notify the cache monitor about the error, and kick off the recovery process.
        RemoteCacheMonitor.getInstance().notifyError();

        // initiate failover if local
        RemoteCacheNoWaitFacade rcnwf = ( RemoteCacheNoWaitFacade ) RemoteCacheFactory.facades.get( irca.getCacheName() );
        log.debug( "Initiating failover, rcnf = " + rcnwf );
        if ( rcnwf != null && rcnwf.rca.getRemoteType() == rcnwf.rca.LOCAL )
        {
            log.debug( "found facade calling failover" );
            // may need to remove the noWait index here. It will be 0 if it is local
            // since there is only 1 possible listener.
            rcnwf.failover( 0 );
        }

        if ( ex instanceof IOException )
        {
            throw ( IOException ) ex;
        }
        throw new IOException( ex.getMessage() );
    }
}
