"""Run through Blender MCP. Non-destructively authors, previews and exports the two creatures."""
import bpy, math, json, random
from pathlib import Path
from mathutils import Vector
ROOT = Path(PROJECT_ROOT)
asset = ROOT / "src/main/resources/assets/mystcraft"
(asset / "models/entity").mkdir(parents=True, exist_ok=True)
(asset / "textures/entity").mkdir(parents=True, exist_ok=True)
original_scene = bpy.data.scenes.get("Scene") if bpy.context.scene.name.startswith("Nasuko Dimensional Monsters") else bpy.context.window.scene
scene = bpy.data.scenes.new("Nasuko Dimensional Monsters")
bpy.context.window.scene = scene
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 1200
scene.render.resolution_y = 800
scene.render.resolution_percentage = 100
scene.world = bpy.data.worlds.new("Nasuko Monster Studio")
scene.world.color = (.035,.045,.07)
scene.view_settings.view_transform = "Standard"
models = {}
def to_bl(v): return Vector((v[0], -v[2], v[1]))
def to_mc(v): return [v.x, v.z, -v.y]
def texture(name, colors):
    image = bpy.data.images.new(name, width=64, height=64)
    rng = random.Random(630025)
    pixels=[]
    for y in range(64):
        for x in range(64):
            tile=(y//32)*2+x//32
            rgb=colors[tile]
            scale=.90 + .16*rng.random()
            if tile==0 and ((x+(y//4)*2)%8==0 or y%5==0): scale=.68
            if tile==1 and (x+y)%11==0: scale=.78
            pixels.extend([min(1,c*scale) for c in rgb]+[1])
    image.pixels=pixels
    image.filepath_raw=str(asset / "textures/entity" / (name+".png"))
    image.file_format="PNG";image.save()
    material=bpy.data.materials.new(name);material.use_nodes=True
    node=material.node_tree.nodes.new("ShaderNodeTexImage");node.image=image;node.interpolation="Closest"
    shader=next((n for n in material.node_tree.nodes if n.type=="BSDF_PRINCIPLED"),None)
    if shader is None: shader=material.node_tree.nodes.new("ShaderNodeBsdfPrincipled")
    output=next((n for n in material.node_tree.nodes if n.type=="OUTPUT_MATERIAL"),None)
    if output is None: output=material.node_tree.nodes.new("ShaderNodeOutputMaterial")
    material.node_tree.links.new(shader.outputs["BSDF"],output.inputs["Surface"])
    material.node_tree.links.new(node.outputs["Color"],shader.inputs["Base Color"])
    shader.inputs["Roughness"].default_value=.82
    return material
pteromat=texture("dimensional_pterosaur",[(.18,.12,.29),(.46,.29,.64),(.23,.94,.91),(.84,.81,.66)])
snakemat=texture("dimensional_serpent",[(.10,.18,.23),(.30,.47,.48),(.65,.98,.45),(.83,.77,.52)])
def model(name, offset, material):
    collection=bpy.data.collections.new(name);scene.collection.children.link(collection)
    root=bpy.data.objects.new(name+"_root",None);collection.objects.link(root);root.location=to_bl(offset)
    models[name]={"root":root,"collection":collection,"material":material,"bones":{}}
    return name
def bone(model,name,pivot):
    m=models[model]
    if name not in m["bones"]:
        obj=bpy.data.objects.new(model+"_"+name,None);m["collection"].objects.link(obj);obj.parent=m["root"];obj.location=to_bl(pivot)
        obj["mc_pivot"]=pivot;obj["bone_name"]=name;m["bones"][name]=obj
    return m["bones"][name]
def finish(obj, model, part, pivot, tile):
    m=models[model];parent=bone(model,part,pivot)
    # Object locations and mesh vertices are authored relative to a dedicated bone.
    for coll in list(obj.users_collection):coll.objects.unlink(obj)
    m["collection"].objects.link(obj)
    obj.parent=parent;obj.location-=to_bl(pivot)
    obj.data.materials.append(m["material"])
    uv=obj.data.uv_layers.new(name="PixelAtlas")
    uv.active_render=True
    obj.data.uv_layers.active_index=len(obj.data.uv_layers)-1
    for poly in obj.data.polygons:
        corners=[(.03,.03),(.47,.03),(.47,.47),(.03,.47)]
        for k,li in enumerate(poly.loop_indices):
            u,v=corners[k%4];uv.data[li].uv=(u+(tile%2)*.5,v+(tile//2)*.5)
    obj["part"]=part;obj["emissive"]=tile==2
    return obj
def box(model,name,center,size,tile=0,part="body",pivot=(0,0,0)):
    bpy.ops.mesh.primitive_cube_add(size=1,location=to_bl(center))
    obj=bpy.context.object;obj.name=model+"_"+name;obj.scale=(size[0],size[2],size[1])
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    return finish(obj,model,part,pivot,tile)
def mesh(model,name,vertices,faces,tile=0,part="body",pivot=(0,0,0)):
    data=bpy.data.meshes.new(model+"_"+name);data.from_pydata([to_bl(v) for v in vertices],[],faces);data.update()
    obj=bpy.data.objects.new(data.name,data);models[model]["collection"].objects.link(obj)
    return finish(obj,model,part,pivot,tile)
def wedge(model,name,center,width,height,length,tile,part="body",pivot=(0,0,0)):
    x,y,z=center
    v=[(x-width/2,y-height/2,z),(x+width/2,y-height/2,z),(x,y-height/3,z-length),
       (x-width/2,y+height/2,z),(x+width/2,y+height/2,z),(x,y+height/3,z-length)]
    return mesh(model,name,v,[(0,2,1),(3,4,5),(0,1,4,3),(1,2,5,4),(2,0,3,5)],tile,part,pivot)
p=model("dimensional_pterosaur",(-2.8,0,0),pteromat)
box(p,"torso",(0,.95,.05),(.57,.56,1.0))
box(p,"breast",(0,.83,-.20),(.48,.19,.65),1)
box(p,"neck",(0,1.13,-.54),(.29,.38,.55))
box(p,"skull",(0,1.28,-.94),(.43,.34,.52))
wedge(p,"beak",(0,1.23,-1.17),.37,.20,.85,3)
wedge(p,"jaw",(0,1.10,-1.14),.30,.075,.70,0,"jaw",(0,1.11,-1.04))
# A swept-back pterosaur crest, not a bat's rounded head.
mesh(p,"crest",[(-.055,1.39,-.95),(.055,1.39,-.95),(-.055,1.90,-.38),(.055,1.90,-.38),(-.055,1.39,-.23),(.055,1.39,-.23)],[(0,2,4),(1,5,3),(0,1,3,2),(2,3,5,4),(4,5,1,0)],1)
for side in [-1,1]:
    label="left" if side<0 else "right";part="wing_"+label;pivot=(side*.25,1.08,-.1)
    vertices=[(.25,1.08,-.25),(.9,1.1,-.65),(2.35,1.05,-.20),(1.85,1.0,.20),(1.05,.88,.75),(.25,.90,.45)]
    vertices=[(side*x,y,z) for x,y,z in vertices]
    # Closed membrane avoids backface holes when wings flap above the camera.
    allv=vertices+[(x,y-.025,z) for x,y,z in vertices]
    faces=[(0,1,2),(0,2,3),(0,3,4),(0,4,5),(6,8,7),(6,9,8),(6,10,9),(6,11,10)]
    faces += [(i,(i+1)%6,(i+1)%6+6,i+6) for i in range(6)]
    # Mirroring changes winding: keep both membranes' top normals facing up.
    if side>0:faces=[tuple(reversed(face)) for face in faces]
    mesh(p,"membrane_"+label,allv,faces,1,part,pivot)
    box(p,"wing_bone_"+label,(side*1.22,1.10,-.33),(1.96,.075,.085),0,part,pivot)
    box(p,"wing_claw_"+label,(side*.92,1.13,-.64),(.09,.09,.20),3,part,pivot)
    box(p,"eye_"+label,(side*.221,1.34,-1.04),(.018,.11,.14),2)
    box(p,"pupil_"+label,(side*.233,1.34,-1.07),(.01,.07,.025),0)
    box(p,"leg_"+label,(side*.19,.54,.27),(.13,.39,.15),0)
    for toe in range(2):box(p,"talon_"+label+str(toe),(side*.19+(toe-.5)*.075,.32,.13),(.05,.06,.27),3)
wedge(p,"tail",(0,.95,.52),.31,.22,-1.15,0,"tail",(0,.95,.52))
s=model("dimensional_serpent",(2.15,0,0),snakemat)
box(s,"head",(0,.34,-.63),(.53,.28,.57),0,"head",(0,.30,-.35))
wedge(s,"nose",(0,.33,-.85),.51,.22,.26,0,"head",(0,.30,-.35))
box(s,"jaw",(0,.21,-.67),(.43,.09,.58),1,"jaw",(0,.25,-.37))
for side in [-1,1]:
    box(s,"eye"+str(side),(side*.266,.40,-.76),(.018,.09,.14),2,"head",(0,.30,-.35))
    box(s,"pupil"+str(side),(side*.277,.40,-.79),(.008,.075,.025),0,"head",(0,.30,-.35))
    wedge(s,"fang"+str(side),(side*.14,.19,-.80),.05,.08,.11,3,"jaw",(0,.25,-.37))
for i in range(12):
    z=-.25+i*.245;width=max(.065,.42-i*.027);height=max(.065,.31-i*.019)
    pivot=(0,height/2+.02,z);part="segment_"+str(i)
    box(s,"scales"+str(i),pivot,(width,height,.30),0,part,pivot)
    box(s,"belly"+str(i),(0,.045,z),(width*.78,.055,.31),1,part,pivot)
    if i<8:box(s,"rift_ridge"+str(i),(0,height+.04,z),(.065,.055,.10),2,part,pivot)
# Export actual Blender meshes in the neutral frame into a small Minecraft render format.
scene.frame_set(0);bpy.context.view_layer.update()
summary={}
for name,m in models.items():
    parts=[]
    for obj in m["collection"].objects:
        if obj.type!="MESH":continue
        part=obj["part"];pivot=list(m["bones"][part]["mc_pivot"]);quads=[]
        for poly in obj.data.polygons:
            indices=list(poly.vertices);loops=list(poly.loop_indices)
            groups=[list(range(len(indices)))] if len(indices)<=4 else [[0,i,i+1] for i in range(1,len(indices)-1)]
            for group in groups:
                if len(group)==3:group=group+[group[-1]]
                quad=[]
                n=to_mc((obj.matrix_world.to_3x3()@poly.normal).normalized())
                for k in group:
                    local=obj.matrix_local@obj.data.vertices[indices[k]].co
                    xyz=to_mc(local);uv=obj.data.uv_layers.active.data[loops[k]].uv
                    quad.append([round(v,6) for v in xyz+[uv.x,1-uv.y]+n])
                quads.append(quad)
        parts.append({"bone":part,"pivot":pivot,"emissive":bool(obj["emissive"]),"quads":quads})
    (asset/"models/entity"/(name+".json")).write_text(json.dumps({"format":1,"parts":parts},separators=(",",":"))+"\n",encoding="utf-8")
    summary[name]={"parts":len(parts),"quads":sum(len(x["quads"]) for x in parts)}
# Author reusable Blender preview animation on the same bone objects.
for side,sign in [("left",-1),("right",1)]:
    obj=models[p]["bones"]["wing_"+side]
    for frame,angle in [(0,0),(5,32),(15,-40),(25,32),(35,-40),(40,0)]:
        obj.rotation_euler[1]=math.radians(angle*sign);obj.keyframe_insert(data_path="rotation_euler",frame=frame)
for i in range(12):
    obj=models[s]["bones"]["segment_"+str(i)]
    for frame in [0,10,20,30,40]:
        obj.location.x=math.sin(frame*math.pi/20-i*.55)*.08;obj.keyframe_insert(data_path="location",frame=frame)
scene.frame_start=0;scene.frame_end=40;scene.frame_set(0)
# Dedicated studio objects; existing user objects/scenes are never removed or altered.
camdata=bpy.data.cameras.new("Nasuko Monster Preview");camera=bpy.data.objects.new(camdata.name,camdata);scene.collection.objects.link(camera)
camera.location=(7,-11,7);camera.rotation_euler=(Vector((0,-.2,.65))-camera.location).to_track_quat('-Z','Y').to_euler();camdata.type='ORTHO';camdata.ortho_scale=9.5;scene.camera=camera
for pos,energy,size in [((1,-4,7),1200,6),((-5,1,4),900,5)]:
    lightdata=bpy.data.lights.new("Nasuko Studio Softbox",'AREA');lightdata.energy=energy;lightdata.shape='DISK';lightdata.size=size
    light=bpy.data.objects.new(lightdata.name,lightdata);scene.collection.objects.link(light);light.location=pos;light.rotation_euler=(Vector((0,0,1))-light.location).to_track_quat('-Z','Y').to_euler()
scene.render.filepath=str(ROOT/"build/verification/dimensional-monsters/blender-preview.png")
bpy.data.libraries.write(str(ROOT/"art/dimensional_monsters.blend"),{scene},fake_user=True,compress=True,path_remap='RELATIVE_ALL')
bpy.ops.render.render(write_still=True)
bpy.context.window.scene=original_scene
result={"status":"ok","models":summary,"blend":str(ROOT/"art/dimensional_monsters.blend"),"preview":scene.render.filepath}
