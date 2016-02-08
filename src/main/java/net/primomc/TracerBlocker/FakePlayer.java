package net.primomc.TracerBlocker;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import net.primomc.TracerBlocker.PacketWrapper.WrapperPlayServerEntityDestroy;
import net.primomc.TracerBlocker.PacketWrapper.WrapperPlayServerEntityMoveLook;
import net.primomc.TracerBlocker.PacketWrapper.WrapperPlayServerNamedEntitySpawn;
import net.primomc.TracerBlocker.PacketWrapper.WrapperPlayServerPlayerInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FakePlayer
{
    /**
     * Retrieve the entity counter field used to generate a unique entity ID.
     */
    private static final FieldAccessor ENTITY_ID = Accessors.getFieldAccessor(
            MinecraftReflection.getEntityClass(), "entityCount", true );
    private final Vector vector;

    public List<Player> observers = Lists.newArrayList();

    private Location clientLocation;
    private Location serverLocation;

    private int entityId;
    private String name;
    private UUID uuid;
    private WrappedDataWatcher watcher;
    private boolean changed;


    // Update task
    private BukkitTask task;

    public FakePlayer( final Plugin plugin, Location location )
    {
        this.clientLocation = Preconditions.checkNotNull( location, "location cannot be NULL" );
        this.serverLocation = clientLocation.clone();
        Vector v = Vector.getRandom(); // This makes a random vector but it doesn't shoot all ways //
        v.setX( v.getX() - 0.5f );
        v.setZ( v.getZ() - 0.5f ); // Now it does //
        v.setY( v.getY() / 5 );
        vector = v.clone();
        this.name = RandomNameGenerator.getRandomName();
        this.uuid = UUID.randomUUID();
        this.entityId = (Integer) ENTITY_ID.get( null );
        this.watcher = new WrappedDataWatcher();
        watcher.setObject( 0, (byte) 0x20 );
        watcher.setObject( 1, (short) 300 );
        watcher.setObject( 2, name );
        watcher.setObject( 6, (float) 20 );
        // Increment next entity ID
        ENTITY_ID.set( null, entityId + 1 );
        // Background worker
        task = new BukkitRunnable()
        {
            int i = 0;

            @Override
            public void run()
            {
                if ( i > Settings.FakePlayers.secondsAlive * ( 20 / Settings.FakePlayers.speed ) )
                {
                    destroy();
                    return;
                }
                moveEntity();
                updateEntity();
                i++;
            }
        }.runTaskTimer( plugin, 1, Settings.FakePlayers.speed );
    }

    private void moveEntity()
    {
        serverLocation.add( vector.getX() / 100, vector.getY() / 100, vector.getZ() / 100 );
    }

    public void addObserver( Player player )
    {
        notifySpawnEntity( player );
        observers.add( player );
    }

    public void removeObserver( Player player )
    {
        sendRemovePlayerTab( player );
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds( new int[]{ entityId } );
        destroy.sendPacket( player );
        observers.remove( player );
    }

    private void updateEntity()
    {
        // Detect changes
        if ( changed )
        {
            for ( Player player : observers )
            {
                notifySpawnEntity( player );
            }
            changed = false;

            // Update location
        }
        else if ( !serverLocation.equals( clientLocation ) )
        {
            for ( Player player : observers )
            {
                if ( !player.getLocation().getWorld().equals( serverLocation.getWorld() ) )
                {
                    continue;
                }
                if ( player.getLocation().distance( serverLocation ) < 16 )
                {
                    destroy();
                    return;
                }
            }
            broadcastMoveEntity();
            clientLocation = serverLocation.clone();
        }
    }

    private void notifySpawnEntity( Player player )
    {
        sendAddPlayerTab( player );
        WrapperPlayServerNamedEntitySpawn spawned = new WrapperPlayServerNamedEntitySpawn();
        spawned.setEntityID( entityId );
        spawned.setPosition( serverLocation.toVector() );
        spawned.setPlayerUUID( uuid );
        spawned.setYaw( serverLocation.getYaw() );
        spawned.setPitch( serverLocation.getPitch() );
        spawned.setMetadata( watcher );
        spawned.sendPacket( player );
        sendRemovePlayerTab( player );
    }

    private void sendAddPlayerTab( Player player )
    {
        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
        info.setAction( EnumWrappers.PlayerInfoAction.ADD_PLAYER );
        List<PlayerInfoData> dataList = new ArrayList<>();
        WrappedGameProfile profile = new WrappedGameProfile( uuid, name );
        WrappedChatComponent displayName = WrappedChatComponent.fromText( name );

        PlayerInfoData data = new PlayerInfoData( profile, 42, EnumWrappers.NativeGameMode.SURVIVAL, displayName );
        Object generic = PlayerInfoData.getConverter().getGeneric( MinecraftReflection.getPlayerInfoDataClass(), data );
        PlayerInfoData back = PlayerInfoData.getConverter().getSpecific( generic );
        dataList.add( back );
        info.setData( dataList );
        info.sendPacket( player );
    }

    private void sendRemovePlayerTab( Player player )
    {
        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
        info.setAction( EnumWrappers.PlayerInfoAction.REMOVE_PLAYER );
        List<PlayerInfoData> dataList = new ArrayList<>();
        WrappedGameProfile profile = new WrappedGameProfile( uuid, name );
        WrappedChatComponent displayName = WrappedChatComponent.fromText( name );

        PlayerInfoData data = new PlayerInfoData( profile, 42, EnumWrappers.NativeGameMode.SURVIVAL, displayName );
        Object generic = PlayerInfoData.getConverter().getGeneric( MinecraftReflection.getPlayerInfoDataClass(), data );
        PlayerInfoData back = PlayerInfoData.getConverter().getSpecific( generic );
        dataList.add( back );
        info.setData( dataList );
        info.sendPacket( player );
    }

    private void broadcastMoveEntity()
    {
        WrapperPlayServerEntityMoveLook move = new WrapperPlayServerEntityMoveLook();
        move.setEntityID( entityId );
        move.setDx( serverLocation.getX() - clientLocation.getX() );
        move.setDy( serverLocation.getY() - clientLocation.getY() );
        move.setDz( serverLocation.getZ() - clientLocation.getZ() );
        move.setYaw( serverLocation.getYaw() );
        move.setPitch( serverLocation.getPitch() );

        for ( Player player : observers )
        {
            move.sendPacket( player );
        }
    }

    /**
     * Destroy the current entity.
     */
    public void destroy()
    {
        task.cancel();

        for ( Player player : Lists.newArrayList( observers ) )
        {
            removeObserver( player );
        }
    }

    public int getEntityId()
    {
        return entityId;
    }

    /**
     * Retrieve an immutable view of every player observing this entity.
     *
     * @return Every observer.
     */
    public List<Player> getObservers()
    {
        return Collections.unmodifiableList( observers );
    }

    public Location getLocation()
    {
        return serverLocation;
    }

    public void setLocation( Location location )
    {
        this.serverLocation = location;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
        this.changed = true;
    }
}