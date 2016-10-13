package net.primomc.TracerBlocker.FakePlayer;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.*;
import net.primomc.TracerBlocker.PacketWrapper.v1_9.WrapperPlayServerEntityDestroy;
import net.primomc.TracerBlocker.PacketWrapper.v1_9.WrapperPlayServerNamedEntitySpawn;
import net.primomc.TracerBlocker.PacketWrapper.v1_9.WrapperPlayServerPlayerInfo;
import net.primomc.TracerBlocker.PacketWrapper.v1_9.WrapperPlayServerRelEntityMoveLook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings( "Duplicates" )
public class FakePlayer1_9 extends AbstractFakePlayer
{

    public FakePlayer1_9( Plugin plugin, Location location )
    {
        super( plugin, location );
    }

    protected WrappedDataWatcher getWatcher( Player player, WrapperPlayServerNamedEntitySpawn packet )
    {
        WrappedDataWatcher watcher = new WrappedDataWatcher();

        watcher.setObject( 0, WrappedDataWatcher.Registry.get( Byte.class ), (byte) 0x20 );
        watcher.setObject( 1, WrappedDataWatcher.Registry.get( Integer.class ), 300 );
        watcher.setObject( 2, WrappedDataWatcher.Registry.get( String.class ), name );
        watcher.setObject( 6, WrappedDataWatcher.Registry.get( Float.class ), (float) 20 );
        return watcher;
    }

    @Override
    protected void removeObserver( Player player )
    {
        sendRemovePlayerTab( player );
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds( new int[]{ entityId } );
        destroy.sendPacket( player );
        observers.remove( player );
    }

    protected void notifySpawnEntity( Player player )
    {
        sendAddPlayerTab( player );
        WrapperPlayServerNamedEntitySpawn spawned = new WrapperPlayServerNamedEntitySpawn();
        spawned.setEntityID( entityId );
        spawned.setPosition( serverLocation.toVector() );
        spawned.setPlayerUUID( uuid );
        spawned.setYaw( serverLocation.getYaw() );
        spawned.setPitch( serverLocation.getPitch() );
        spawned.setMetadata( getWatcher( player, spawned ) );
        spawned.sendPacket( player );
        sendRemovePlayerTab( player );
    }

    protected void sendAddPlayerTab( Player player )
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

    protected void sendRemovePlayerTab( Player player )
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

    protected void broadcastMoveEntity()
    {
        WrapperPlayServerRelEntityMoveLook move = new WrapperPlayServerRelEntityMoveLook();
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

}