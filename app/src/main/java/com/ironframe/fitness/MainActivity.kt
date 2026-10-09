package com.ironframe.fitness

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF151716)
private val Steel = Color(0xFF252928)
private val Lime = Color(0xFFD7F36A)
private val Chalk = Color(0xFFF0EEE6)
private val Ash = Color(0xFF9DA39A)

data class Frame(val id:String,val path:String,val time:Long,val note:String)
data class Album(val id:String,val name:String,val pose:String,val frames:List<Frame> = emptyList())

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  window.statusBarColor=android.graphics.Color.rgb(21,23,22); window.navigationBarColor=android.graphics.Color.rgb(21,23,22)
  setContent { StudioApp() }
 }
}

@Composable private fun StudioApp() {
 val ctx= LocalContext.current
 val prefs= remember { ctx.getSharedPreferences("ironframe",Context.MODE_PRIVATE) }
 fun load():List<Album> = try {
  val arr=JSONArray(prefs.getString("albums","[]"))
  (0 until arr.length()).map { i -> val a=arr.getJSONObject(i); val fs=a.getJSONArray("frames")
   Album(a.getString("id"),a.getString("name"),a.getString("pose"),(0 until fs.length()).map { j -> val f=fs.getJSONObject(j); Frame(f.getString("id"),f.getString("path"),f.getLong("time"),f.optString("note")) })
  }
 } catch(_:Exception){emptyList()}
 var albums by remember { mutableStateOf(load()) }
 fun save(list:List<Album>) { albums=list; val arr=JSONArray(); list.forEach { a -> val fs=JSONArray(); a.frames.forEach { f -> fs.put(JSONObject().put("id",f.id).put("path",f.path).put("time",f.time).put("note",f.note)) }; arr.put(JSONObject().put("id",a.id).put("name",a.name).put("pose",a.pose).put("frames",fs)) }; prefs.edit().putString("albums",arr.toString()).apply() }
 var currentId by remember { mutableStateOf<String?>(null) }
 var tab by remember { mutableStateOf("Capture") }
 var create by remember { mutableStateOf(false) }
 var name by remember { mutableStateOf("") }
 var pose by remember { mutableStateOf("Front relaxed") }
 var note by remember { mutableStateOf("") }
 var ghostOn by remember { mutableStateOf(true) }
 var opacity by remember { mutableFloatStateOf(.3f) }
 var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) }
 var capture by remember { mutableStateOf<ImageCapture?>(null) }
 var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
 var playing by remember { mutableStateOf(false) }
 var frameIndex by remember { mutableIntStateOf(0) }
 var deleting by remember { mutableStateOf<Frame?>(null) }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it}
 val selected=albums.firstOrNull { it.id==currentId }
 MaterialTheme(colorScheme=darkColorScheme(primary=Lime,background=Ink,surface=Steel,onSurface=Chalk,onPrimary=Ink)) {
  Surface(Modifier.fillMaxSize(),color=Ink) {
   if(selected==null) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically) { Text("IF",color=Lime,fontSize=26.sp,fontWeight=FontWeight.Black); Spacer(Modifier.width(12.dp)); Column { Text("IRONFRAME",color=Chalk,fontWeight=FontWeight.Black,letterSpacing=2.sp); Text("NATIVE PROGRESS STUDIO",color=Ash,fontSize=9.sp,letterSpacing=1.sp) } }
     Spacer(Modifier.height(32.dp)); Text("YOUR WORK. YOUR PROOF.",color=Lime,fontSize=10.sp,letterSpacing=2.sp)
     Text("Built day by day.",color=Chalk,fontSize=37.sp,fontWeight=FontWeight.Black)
     Text("A private visual log of the work you're putting in.",color=Ash,fontSize=12.sp)
     Spacer(Modifier.height(22.dp))
     Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Steel).padding(16.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
      Column(horizontalAlignment=Alignment.CenterHorizontally){Text(albums.size.toString(),color=Lime,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("ALBUMS",color=Ash,fontSize=9.sp)}
      Column(horizontalAlignment=Alignment.CenterHorizontally){Text(albums.sumOf{it.frames.size}.toString(),color=Lime,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("FRAMES",color=Ash,fontSize=9.sp)}
      Column(horizontalAlignment=Alignment.CenterHorizontally){Text(albums.flatMap{it.frames}.map{it.time/86400000}.distinct().size.toString(),color=Lime,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("ACTIVE DAYS",color=Ash,fontSize=9.sp)}
     }
     Spacer(Modifier.height(26.dp))
     Row(verticalAlignment=Alignment.CenterVertically){Text("Your albums",color=Chalk,fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f)); Button(onClick={create=true},colors=ButtonDefaults.buttonColors(containerColor=Lime)){Icon(Icons.Default.Add,null);Text("New album")}}
     if(albums.isEmpty()) { Spacer(Modifier.height(15.dp)); Column(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(Steel).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.Folder,null,tint=Lime,modifier=Modifier.size(44.dp));Text("Start your first album",color=Chalk,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Create an album for each pose. Your first frame becomes the alignment reference.",color=Ash,fontSize=12.sp);Button(onClick={create=true},colors=ButtonDefaults.buttonColors(containerColor=Lime)){Text("CREATE FIRST ALBUM")}} }
     else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){items(albums,key={it.id}){a->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Steel).clickable{currentId=a.id;tab="Capture"}.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.PhotoLibrary,null,tint=Lime,modifier=Modifier.size(35.dp));Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(a.name,color=Chalk,fontWeight=FontWeight.Bold,fontSize=17.sp);Text(a.pose.uppercase(),color=Lime,fontSize=9.sp,letterSpacing=1.sp);Text(a.frames.size.toString()+" frames",color=Ash,fontSize=11.sp)};Text("›",color=Lime,fontSize=26.sp)}}}
     Spacer(Modifier.height(12.dp));Text("▣  YOUR BODY. YOUR DATA. Photos stay on this device.",color=Ash,fontSize=10.sp)
    }
   } else {
    Column(Modifier.fillMaxSize().padding(horizontal=14.dp)) {
     Row(Modifier.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={currentId=null;playing=false}){Icon(Icons.Default.ArrowBack,null,tint=Chalk)};Column(Modifier.weight(1f)){Text("PROGRESS ALBUM",color=Lime,fontSize=9.sp,letterSpacing=2.sp);Text(selected.name,color=Chalk,fontSize=23.sp,fontWeight=FontWeight.Bold)};Text(selected.frames.size.toString()+" frames",color=Ash,fontSize=10.sp)}
     Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Steel).padding(4.dp)){listOf("Capture","Gallery","Timelapse").forEach{t->Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if(tab==t)Color(0xFF3B472A)else Color.Transparent).clickable{tab=t;playing=false}.padding(11.dp),Alignment.Center){Text(t,color=if(tab==t)Lime else Ash,fontSize=12.sp,fontWeight=FontWeight.Bold)}}}
     when(tab){
      "Capture" -> {
       val previous=selected.frames.lastOrNull()
       Column(Modifier.fillMaxSize().padding(top=12.dp,bottom=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B0D0C)).border(1.dp,Color(0xFF51594B),RoundedCornerShape(16.dp))){
         if(granted) AndroidView(Modifier.fillMaxSize(),factory={c->val pv=PreviewView(c);val future=ProcessCameraProvider.getInstance(c);future.addListener({val provider=future.get();val preview=Preview.Builder().build().also{it.setSurfaceProvider(pv.surfaceProvider)};val ic=ImageCapture.Builder().build();try{provider.unbindAll();provider.bindToLifecycle(c as ComponentActivity,CameraSelector.Builder().requireLensFacing(lens).build(),preview,ic);capture=ic}catch(_:Exception){}},ContextCompat.getMainExecutor(c));pv})
         else Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.PhotoCamera,null,tint=Lime,modifier=Modifier.size(40.dp));Text("Camera permission needed",color=Chalk);Button(onClick={permission.launch(Manifest.permission.CAMERA)},colors=ButtonDefaults.buttonColors(containerColor=Lime)){Text("Enable camera")}}
         if(granted&&ghostOn&&previous!=null){val bmp=remember(previous.path){BitmapFactory.decodeFile(previous.path)};if(bmp!=null)Image(bmp.asImageBitmap(),null,Modifier.fillMaxSize(),alpha=opacity)}
         Box(Modifier.align(Alignment.Center).fillMaxHeight().width(1.dp).background(Lime.copy(alpha=.25f)));Box(Modifier.align(Alignment.Center).fillMaxWidth().height(1.dp).background(Lime.copy(alpha=.25f)))
         Text(if(previous==null)"FIRST FRAME" else "GHOST ALIGNMENT READY",Modifier.align(Alignment.TopStart).padding(10.dp).background(Ink.copy(alpha=.8f),RoundedCornerShape(5.dp)).padding(7.dp),color=Lime,fontSize=9.sp,letterSpacing=1.sp)
         IconButton(onClick={lens=if(lens==CameraSelector.LENS_FACING_BACK)CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK;capture=null},modifier=Modifier.align(Alignment.TopEnd)){Text("↻",color=Lime,fontSize=23.sp)}
        }
        if(previous!=null){Row(verticalAlignment=Alignment.CenterVertically){Text("Ghost opacity",color=Ash,fontSize=10.sp);Slider(opacity,{opacity=it},Modifier.weight(1f),valueRange=0f..0.8f,colors=SliderDefaults.colors(thumbColor=Lime,activeTrackColor=Lime));Text((opacity*100).toInt().toString()+"%",color=Chalk,fontSize=10.sp)};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(ghostOn,{ghostOn=it},colors=CheckboxDefaults.colors(checkedColor=Lime));Text("Show previous photo overlay",color=Ash,fontSize=11.sp)}}
        OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("Note (optional)")},placeholder={Text("Weight, lighting, training notes…")},maxLines=2)
        Spacer(Modifier.height(8.dp));Button(onClick={val ic=capture?:return@Button;val dir=File(ctx.filesDir,"progress_photos/"+selected.id).apply{mkdirs()};val file=File(dir,"frame_"+System.currentTimeMillis()+".jpg");ic.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(ctx),object:ImageCapture.OnImageSavedCallback{override fun onImageSaved(r:ImageCapture.OutputFileResults){val f=Frame(System.currentTimeMillis().toString(),file.absolutePath,System.currentTimeMillis(),note);save(albums.map{if(it.id==selected.id)it.copy(frames=it.frames+f)else it});note="";tab="Gallery"};override fun onError(e:ImageCaptureException){android.widget.Toast.makeText(ctx,"Photo capture failed",android.widget.Toast.LENGTH_SHORT).show()}})},enabled=granted&&capture!=null,Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=Lime,contentColor=Ink),shape=RoundedCornerShape(12.dp)){Icon(Icons.Default.PhotoCamera,null);Text("CAPTURE FRAME",fontWeight=FontWeight.Black,letterSpacing=1.sp)}
        Text("Same distance · same pose · same light",color=Ash,fontSize=10.sp,modifier=Modifier.padding(6.dp))
       }
      }
      "Gallery" -> if(selected.frames.isEmpty())Box(Modifier.fillMaxSize(),Alignment.Center){Text("Capture your first frame to start your history.",color=Ash)} else LazyColumn(contentPadding=PaddingValues(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(selected.frames.reversed(),key={it.id}){f->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Steel).padding(10.dp),verticalAlignment=Alignment.CenterVertically){val bmp=remember(f.path){BitmapFactory.decodeFile(f.path)};if(bmp!=null)Image(bmp.asImageBitmap(),null,Modifier.size(78.dp).clip(RoundedCornerShape(8.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(SimpleDateFormat("dd MMM yyyy · hh:mm a",Locale.getDefault()).format(Date(f.time)),color=Chalk,fontWeight=FontWeight.Bold,fontSize=12.sp);Text(f.note.ifBlank{"Progress frame"},color=Ash,fontSize=11.sp)};IconButton(onClick={deleting=f}){Icon(Icons.Default.Delete,null,tint=Color(0xFFEF8278))}}}}
      else -> {
       LaunchedEffect(playing,selected.id){while(playing&&selected.frames.isNotEmpty()){delay(800);frameIndex=(frameIndex+1)%selected.frames.size}}
       Column(Modifier.fillMaxSize().padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(Steel),Alignment.Center){val f=selected.frames.getOrNull(frameIndex);val bmp=remember(f?.path){f?.let{BitmapFactory.decodeFile(it.path)}};if(bmp!=null)Image(bmp.asImageBitmap(),null,Modifier.fillMaxSize());else Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Movie,null,tint=Lime,modifier=Modifier.size(45.dp));Text("Your story, frame by frame",color=Chalk)};Text((if(selected.frames.isEmpty())0 else frameIndex+1).toString()+" / "+selected.frames.size,Modifier.align(Alignment.BottomEnd).padding(12.dp).background(Ink.copy(alpha=.8f),RoundedCornerShape(6.dp)).padding(8.dp),color=Chalk,fontSize=11.sp)}
       Spacer(Modifier.height(18.dp));Text("TIMELAPSE PREVIEW",color=Lime,fontSize=10.sp,letterSpacing=2.sp);Text("Every frame tells the story.",color=Ash,fontSize=12.sp);Spacer(Modifier.height(12.dp));Button(onClick={if(selected.frames.isNotEmpty()){playing=!playing;if(!playing)frameIndex=0}},enabled=selected.frames.isNotEmpty(),Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=Lime,contentColor=Ink)){Icon(Icons.Default.Movie,null);Text(if(playing)"STOP SEQUENCE" else "▶ PLAY SEQUENCE",fontWeight=FontWeight.Black)};Text("Native video export can be added next; sequence playback works offline.",color=Ash,fontSize=10.sp,modifier=Modifier.padding(10.dp))
       }
      }
     }
    }
   }
   if(create) AlertDialog(onDismissRequest={create=false},containerColor=Steel,title={Text("Create album",color=Chalk)},text={Column{OutlinedTextField(name,{name=it},label={Text("Album name")},singleLine=true);Spacer(Modifier.height(8.dp));Text("POSE / ANGLE",color=Lime,fontSize=10.sp);listOf("Front relaxed","Side","Back","Front double biceps","Custom pose").forEach{p->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(pose==p,{pose=p},colors=RadioButtonDefaults.colors(selectedColor=Lime));Text(p,color=Chalk,fontSize=12.sp)}}}},confirmButton={TextButton(onClick={val n=name.trim().ifBlank{pose};val a=Album(System.currentTimeMillis().toString(),n,pose);save(albums+a);currentId=a.id;tab="Capture";name="";create=false}){Text("CREATE",color=Lime)}},dismissButton={TextButton(onClick={create=false}){Text("CANCEL",color=Ash)}})
   deleting?.let{f->AlertDialog(onDismissRequest={deleting=null},containerColor=Steel,title={Text("Delete this frame?",color=Chalk)},text={Text("This removes the photo from this album permanently.",color=Ash)},confirmButton={TextButton(onClick={save(albums.map{a->if(a.id==currentId)a.copy(frames=a.frames.filterNot{it.id==f.id})else a});File(f.path).delete();deleting=null}){Text("DELETE",color=Color.Red)}},dismissButton={TextButton(onClick={deleting=null}){Text("CANCEL",color=Ash)}})}
  }
 }
}