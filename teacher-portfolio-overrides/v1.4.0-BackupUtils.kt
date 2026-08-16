package com.aborayan.teacherportfolio

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtils {
    fun export(context: Context, db: PortfolioDb, outUri: Uri) {
        val profile=db.getProfile(); val sections=db.getSections(true); val evidence=db.getAllEvidence(); val attachments=db.getAllAttachments()
        val root=JSONObject().apply {
            put("version",2); put("exportedAt",System.currentTimeMillis())
            put("profile",JSONObject().apply {
                put("name",profile.name);put("subject",profile.subject);put("school",profile.school);put("admin",profile.admin);put("year",profile.year)
                put("phone",profile.phone);put("email",profile.email);put("exp",profile.exp);put("bio",profile.bio);put("photoPath",profile.photoPath)
            })
            put("sections",JSONArray().apply { sections.forEach { s -> put(JSONObject().apply { put("id",s.id);put("title",s.title);put("description",s.description);put("hidden",s.hidden);put("isDefault",s.isDefault);put("sortOrder",s.sortOrder);put("status",s.status) }) } })
            put("evidence",JSONArray().apply { evidence.forEach { e -> put(JSONObject().apply { put("id",e.id);put("sectionId",e.sectionId);put("type",e.type);put("title",e.title);put("date",e.date);put("description",e.description);put("link",e.link);put("filePath",e.filePath);put("fileName",e.fileName);put("mimeType",e.mimeType);put("createdAt",e.createdAt);put("sortOrder",e.sortOrder) }) } })
            put("attachments",JSONArray().apply { attachments.forEach { a -> put(JSONObject().apply {put("id",a.id);put("evidenceId",a.evidenceId);put("filePath",a.filePath);put("fileName",a.fileName);put("mimeType",a.mimeType);put("sortOrder",a.sortOrder)}) } })
            put("appearance",AppearanceSettings.exportJson(context))
        }
        context.contentResolver.openOutputStream(outUri)?.use { raw -> ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
            zip.putNextEntry(ZipEntry("data.json")); zip.write(root.toString(2).toByteArray(Charsets.UTF_8)); zip.closeEntry()
            val userRoot=File(context.filesDir,"userfiles")
            if(userRoot.exists()) userRoot.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel=f.relativeTo(userRoot).invariantSeparatorsPath
                zip.putNextEntry(ZipEntry("userfiles/$rel")); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }} ?: error("تعذر فتح ملف النسخة الاحتياطية")
    }

    fun restore(context: Context, db: PortfolioDb, inUri: Uri) {
        val temp=File(context.cacheDir,"restore_${System.currentTimeMillis()}").apply { mkdirs() }
        var jsonText:String?=null
        context.contentResolver.openInputStream(inUri)?.use { raw -> ZipInputStream(BufferedInputStream(raw)).use { zin ->
            while(true) {
                val entry=zin.nextEntry ?: break
                val safe=entry.name.replace('\\','/')
                if(safe=="data.json") { val out=ByteArrayOutputStream(); zin.copyTo(out); jsonText=out.toString(Charsets.UTF_8.name()) }
                else if(safe.startsWith("userfiles/") && !safe.contains("..")) {
                    val rel=safe.removePrefix("userfiles/"); val dest=File(temp,rel); val root=temp.canonicalFile; val target=dest.canonicalFile
                    if(!target.path.startsWith(root.path+File.separator)) error("مسار غير صالح داخل النسخة الاحتياطية")
                    target.parentFile?.mkdirs(); FileOutputStream(target).use{zin.copyTo(it)}
                }
                zin.closeEntry()
            }
        }} ?: error("تعذر قراءة النسخة الاحتياطية")
        val root=JSONObject(jsonText ?: error("ملف data.json غير موجود"))
        val liveRoot=File(context.filesDir,"userfiles"); liveRoot.deleteRecursively(); liveRoot.mkdirs(); temp.copyRecursively(liveRoot,overwrite=true); temp.deleteRecursively()
        db.clearAllForRestore()
        val p=root.getJSONObject("profile")
        db.saveProfile(Profile(p.optString("name"),p.optString("subject"),p.optString("school"),p.optString("admin"),p.optString("year"),p.optString("phone"),p.optString("email"),p.optString("exp"),p.optString("bio"),p.optString("photoPath")))
        val ss=root.getJSONArray("sections"); for(i in 0 until ss.length()){val s=ss.getJSONObject(i);db.upsertSection(Section(s.getString("id"),s.getString("title"),s.optString("description"),s.optBoolean("hidden"),s.optBoolean("isDefault"),s.optInt("sortOrder"),s.optString("status","غير مكتمل")))}
        val es=root.getJSONArray("evidence"); for(i in 0 until es.length()){val e=es.getJSONObject(i);db.upsertEvidence(Evidence(e.getString("id"),e.getString("sectionId"),e.getString("type"),e.getString("title"),e.optString("date"),e.optString("description"),e.optString("link"),e.optString("filePath"),e.optString("fileName"),e.optString("mimeType"),e.optLong("createdAt"),e.optInt("sortOrder",i)))}
        val aa=root.optJSONArray("attachments")
        if(aa!=null){for(i in 0 until aa.length()){val a=aa.getJSONObject(i);db.upsertAttachment(Attachment(a.optString("id"),a.optString("evidenceId"),a.optString("filePath"),a.optString("fileName"),a.optString("mimeType"),a.optInt("sortOrder",i)))}}
        else db.getAllEvidence().filter{it.filePath.isNotBlank()}.forEachIndexed{i,e->db.upsertAttachment(Attachment(java.util.UUID.randomUUID().toString(),e.id,e.filePath,e.fileName,e.mimeType,i))}
        root.optJSONObject("appearance")?.let{AppearanceSettings.restoreJson(context,it)}
    }
}
