//package net.primomc.TracerBlocker.FakePlayer;
//
//import com.comphenix.protocol.utility.MinecraftReflection;
//import com.comphenix.protocol.wrappers.EnumWrappers;
//import com.comphenix.protocol.wrappers.PlayerInfoData;
//import com.comphenix.protocol.wrappers.WrappedChatComponent;
//import com.comphenix.protocol.wrappers.WrappedGameProfile;
//import net.primomc.TracerBlocker.PacketWrapper.v1_8.WrapperPlayServerEntityDestroy;
//import net.primomc.TracerBlocker.PacketWrapper.v1_8.WrapperPlayServerEntityMoveLook;
//import net.primomc.TracerBlocker.PacketWrapper.v1_8.WrapperPlayServerNamedEntitySpawn;
//import net.primomc.TracerBlocker.PacketWrapper.v1_8.WrapperPlayServerPlayerInfo;
//import org.bukkit.Location;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.Plugin;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@SuppressWarnings( "Duplicates" )
//public class FakePlayer1_8 extends AbstractFakePlayer
//{
//
//    public FakePlayer1_8( Plugin plugin, Location location )
//    {
//        super( plugin, location );
//    }
//
//    protected void setWatcher()
//    {
//        watcher.setObject( 0, (byte) 0x20 );
//        watcher.setObject( 1, (short) 300 );
//        watcher.setObject( 2, name );
//        watcher.setObject( 6, (float) 20 );
//    }
//
//
//    @Override
//    protected void removeObserver( Player player )
//    {
//        sendRemovePlayerTab( player );
//        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
//        destroy.setEntityIds( new int[]{ entityId } );
//        destroy.sendPacket( player );
//        observers.remove( player );
//    }
//
//    protected void notifySpawnEntity( Player player )
//    {
//        sendAddPlayerTab( player );
//        WrapperPlayServerNamedEntitySpawn spawned = new WrapperPlayServerNamedEntitySpawn();
//        spawned.setEntityID( entityId );
//        spawned.setPosition( serverLocation.toVector() );
//        spawned.setPlayerUUID( uuid );
//        spawned.setYaw( serverLocation.getYaw() );
//        spawned.setPitch( serverLocation.getPitch() );
//        spawned.setMetadata( watcher );
//        spawned.sendPacket( player );
//        sendRemovePlayerTab( player );
//    }
//
//    protected void sendAddPlayerTab( Player player )
//    {
//        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
//        info.setAction( EnumWrappers.PlayerInfoAction.ADD_PLAYER );
//        List<PlayerInfoData> dataList = new ArrayList<>();
//        WrappedGameProfile profile = new WrappedGameProfile( uuid, name );
//        WrappedChatComponent displayName = WrappedChatComponent.fromText( name );
//
//        PlayerInfoData data = new PlayerInfoData( profile, 42, EnumWrappers.NativeGameMode.SURVIVAL, displayName );
//        Object generic = PlayerInfoData.getConverter().getGeneric( MinecraftReflection.getPlayerInfoDataClass(), data );
//        PlayerInfoData back = PlayerInfoData.getConverter().getSpecific( generic );
//        dataList.add( back );
//        info.setData( dataList );
//        info.sendPacket( player );
//    }
//
//    protected void sendRemovePlayerTab( Player player )
//    {
//        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
//        info.setAction( EnumWrappers.PlayerInfoAction.REMOVE_PLAYER );
//        List<PlayerInfoData> dataList = new ArrayList<>();
//        WrappedGameProfile profile = new WrappedGameProfile( uuid, name );
//        WrappedChatComponent displayName = WrappedChatComponent.fromText( name );
//
//        PlayerInfoData data = new PlayerInfoData( profile, 42, EnumWrappers.NativeGameMode.SURVIVAL, displayName );
//        Object generic = PlayerInfoData.getConverter().getGeneric( MinecraftReflection.getPlayerInfoDataClass(), data );
//        PlayerInfoData back = PlayerInfoData.getConverter().getSpecific( generic );
//        dataList.add( back );
//        info.setData( dataList );
//        info.sendPacket( player );
//    }
//
//    protected void broadcastMoveEntity()
//    {
//        WrapperPlayServerEntityMoveLook move = new WrapperPlayServerEntityMoveLook();
//        move.setEntityID( entityId );
//        move.setDx( serverLocation.getX() - clientLocation.getX() );
//        move.setDy( serverLocation.getY() - clientLocation.getY() );
//        move.setDz( serverLocation.getZ() - clientLocation.getZ() );
//        move.setYaw( serverLocation.getYaw() );
//        move.setPitch( serverLocation.getPitch() );
//
//        for ( Player player : observers )
//        {
//            move.sendPacket( player );
//        }
//    }
//
//}