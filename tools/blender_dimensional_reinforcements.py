"""Run through Blender MCP with PROJECT_ROOT. Creates only a dedicated new scene."""
from pathlib import Path
ROOT = Path(PROJECT_ROOT)
# Reuse the native Blender modeling/export helpers, without touching existing creatures/assets.
helpers = (ROOT / "tools/blender_dimensional_monsters.py").read_text(encoding="utf-8").split('\np=model("dimensional_pterosaur"')[0]
helpers = '\n'.join(line for line in helpers.splitlines() if not line.startswith(('pteromat=','snakemat=')))
exec(compile(helpers, '<Nasuko Blender helpers>', 'exec'))
scene.name = "Nasuko Dimensional Reinforcements"
(ROOT / "art").mkdir(exist_ok=True)
(ROOT / "build/verification/dimensional-reinforcements").mkdir(parents=True, exist_ok=True)
fairymat=texture("dimensional_fairy",[(.27,.10,.40),(.66,.29,.90),(.93,.66,1.0),(.51,.38,.62)])
wormmat=texture("dimensional_worm",[(.18,.12,.24),(.39,.24,.44),(.78,.35,.96),(.77,.67,.52)])
devilmat=texture("dimensional_calamity",[(.23,.08,.32),(.51,.20,.67),(.92,.39,1.0),(.23,.18,.28)])
f=model("dimensional_fairy",(-4,1.3,0),fairymat)
box(f,"robe",(0,.43,0),(.27,.40,.22))
box(f,"hem",(0,.23,0),(.36,.15,.29),1)
box(f,"head",(0,.77,-.015),(.29,.29,.27),3)
box(f,"hair",(0,.89,.025),(.34,.09,.31),0)
box(f,"fringe",(0,.83,-.165),(.32,.10,.045),0)
box(f,"halo",(0,1.02,0),(.24,.035,.24),2)
for side in [-1,1]:
    box(f,"eye"+str(side),(side*.075,.79,-.157),(.045,.055,.012),2)
    box(f,"arm"+str(side),(side*.22,.48,-.04),(.09,.31,.10),3,"arm_"+str(side),(side*.17,.62,0))
    box(f,"boot"+str(side),(side*.09,.12,-.03),(.10,.18,.13),0)
    label="left" if side<0 else "right";part="wing_"+label;pivot=(side*.12,.58,.10)
    v=[(side*.13,.60,.10),(side*.65,1.25,.18),(side*.95,1.05,.19),(side*.63,.57,.16),
       (side*.71,.12,.16),(side*.43,.07,.13)]
    faces=[(0,1,2,3),(0,3,4,5)]
    # Double-sided, closed wings keep the pixel silhouette visible throughout flapping.
    v=v+[(x,y,z+.025) for x,y,z in v]
    faces += [(9,8,7,6),(11,10,9,6)] + [(i,(i+1)%6,(i+1)%6+6,i+6) for i in range(6)]
    mesh(f,"butterfly_"+label,v,faces,1,part,pivot)
    box(f,"wing_spark_"+label,(side*.65,1.04,.155),(.08,.12,.035),2,part,pivot)
w=model("dimensional_worm",(0,0,0),wormmat)
# Octagonal armored body sections with bright intersegment rings.
for i in range(10):
    z=i*.51;rx=max(.28,.86-i*.045);ry=max(.25,.65-i*.028);cy=.76
    part="segment_"+str(i);pivot=(0,cy,z)
    for tag,radius_scale,depth,tile in [("carapace",1,.47,0),("band",1.015,.075,1)]:
        v=[]
        for zz in [z-depth/2,z+depth/2]:
            for a in range(8):
                theta=a*math.tau/8;v.append((math.cos(theta)*rx*radius_scale,cy+math.sin(theta)*ry*radius_scale,zz))
        faces=[tuple(reversed(range(8))),tuple(range(8,16))]+[(a,(a+1)%8,(a+1)%8+8,a+8) for a in range(8)]
        mesh(w,tag+str(i),v,faces,tile,part,pivot)
    if i<8:box(w,"spine"+str(i),(0,cy+ry+.06,z),(.14,.16,.19),2,part,pivot)
# A recessed maw with four radial jaws and eight ivory teeth.
box(w,"maw",(0,.76,-.29),(1.15,.87,.10),0,"head",(0,.76,-.3))
for side in [-1,1]:
    box(w,"jaw_side"+str(side),(side*.72,.76,-.48),(.30,.98,.55),1,"jaw_side_"+str(side),(side*.67,.76,-.28))
    box(w,"jaw_vertical"+str(side),(0,.76+side*.51,-.48),(1.15,.26,.55),1,"jaw_vertical_"+str(side),(0,.76+side*.4,-.28))
    for j in range(2):
        wedge(w,"fang_side"+str(side)+str(j),(side*.51,.53+j*.45,-.68),.17,.19,.39,3,"head",(0,.76,-.3))
        wedge(w,"fang_top"+str(side)+str(j),(-.30+j*.6,.76+side*.37,-.68),.18,.16,.39,3,"head",(0,.76,-.3))
box(w,"maw_glow",(0,.76,-.351),(.42,.39,.025),2,"head",(0,.76,-.3))
c=model("dimensional_calamity",(4,0,0),devilmat)
box(c,"pelvis",(0,1.05,.02),(.74,.43,.46))
box(c,"torso",(0,1.75,0),(.96,.96,.57))
box(c,"chest_sigil",(0,1.90,-.296),(.25,.37,.035),2)
box(c,"neck",(0,2.28,0),(.32,.22,.32))
box(c,"skull",(0,2.57,-.015),(.58,.49,.51))
box(c,"brow",(0,2.67,-.29),(.67,.10,.09),0)
box(c,"jaw",(0,2.37,-.10),(.46,.16,.42),1,"jaw",(0,2.42,.03))
for side in [-1,1]:
    box(c,"eye"+str(side),(side*.17,2.60,-.281),(.17,.075,.025),2)
    wedge(c,"horn"+str(side),(side*.30,2.79,.12),.22,.30,-.36,3)
    box(c,"horn_tip"+str(side),(side*.34,2.95,.25),(.13,.10,.13),1)
    part="arm_"+str(side);pivot=(side*.50,2.14,0)
    box(c,"pauldron"+str(side),(side*.62,2.17,0),(.44,.39,.53),1,part,pivot)
    box(c,"arm"+str(side),(side*.75,1.78,0),(.30,.64,.34),0,part,pivot)
    box(c,"forearm"+str(side),(side*.79,1.39,-.12),(.36,.40,.40),1,part,pivot)
    for j in range(3):wedge(c,"claw"+str(side)+str(j),(side*.79+(j-1)*.105,1.21,-.31),.075,.10,.28,3,part,pivot)
    part="leg_"+str(side);pivot=(side*.25,1.13,0)
    box(c,"thigh"+str(side),(side*.25,.86,.015),(.32,.53,.37),0,part,pivot)
    box(c,"shin"+str(side),(side*.30,.39,.03),(.29,.50,.32),1,part,pivot)
    box(c,"hoof"+str(side),(side*.30,.095,-.12),(.38,.19,.55),3,part,pivot)
    label="left" if side<0 else "right";part="wing_"+label;pivot=(side*.38,2.1,.22)
    v=[(side*.38,2.12,.24),(side*1.25,2.83,.32),(side*2.30,2.55,.38),(side*1.73,1.95,.32),
       (side*1.38,1.34,.28),(side*.85,1.58,.24)]
    v=v+[(x,y,z+.045) for x,y,z in v]
    faces=[(0,1,2,3),(0,3,4,5),(9,8,7,6),(11,10,9,6)]+[(i,(i+1)%6,(i+1)%6+6,i+6) for i in range(6)]
    mesh(c,"demon_membrane_"+label,v,faces,1,part,pivot)
    box(c,"wing_claw_"+label,(side*1.25,2.83,.32),(.13,.24,.15),3,part,pivot)
wedge(c,"tail",(0,1.0,.3),.23,.22,-1.2,0,"tail",(0,1,.3))
o=model("calamity_orb",(4,3.8,0),devilmat)
# Floating faceted core and three orthogonal orbiting fragments.
v=[(0,.42,0),(0,-.42,0),(.32,0,0),(0,0,.32),(-.32,0,0),(0,0,-.32)]
mesh(o,"rift_core",v,[(0,2,3),(0,3,4),(0,4,5),(0,5,2),(1,3,2),(1,4,3),(1,5,4),(1,2,5)],2)
for i in range(6):
    a=i*math.tau/6;box(o,"orbit"+str(i),(.47*math.cos(a),.12*math.sin(a*2),.47*math.sin(a)),(.13,.13,.13),1)
# Export the authored Blender geometry in the same native runtime format.
scene.frame_set(0);bpy.context.view_layer.update();summary={}
for name,m in models.items():
    parts=[]
    for obj in m['collection'].objects:
        if obj.type!='MESH':continue
        part=obj['part'];pivot=list(m['bones'][part]['mc_pivot']);quads=[]
        for poly in obj.data.polygons:
            indices=list(poly.vertices);loops=list(poly.loop_indices)
            groups=[list(range(len(indices)))] if len(indices)<=4 else [[0,i,i+1] for i in range(1,len(indices)-1)]
            for group in groups:
                if len(group)==3:group=group+[group[-1]]
                n=to_mc((obj.matrix_world.to_3x3()@poly.normal).normalized());quad=[]
                for k in group:
                    xyz=to_mc(obj.matrix_local@obj.data.vertices[indices[k]].co);uv=obj.data.uv_layers.active.data[loops[k]].uv
                    quad.append([round(q,6) for q in xyz+[uv.x,1-uv.y]+n])
                quads.append(quad)
        parts.append({'bone':part,'pivot':pivot,'emissive':bool(obj['emissive']),'quads':quads})
    (asset/'models/entity'/(name+'.json')).write_text(json.dumps({'format':1,'parts':parts},separators=(',',':'))+'\n',encoding='utf-8')
    summary[name]={'parts':len(parts),'quads':sum(len(x['quads']) for x in parts)}
for name in [f,c]:
    for side,sign in [('left',-1),('right',1)]:
        obj=models[name]['bones']['wing_'+side]
        for frame,angle in [(0,0),(5,40),(15,-45),(25,40),(35,-45),(40,0)]:
            obj.rotation_euler[2]=math.radians(angle*sign);obj.keyframe_insert(data_path='rotation_euler',frame=frame)
for i in range(10):
    obj=models[w]['bones']['segment_'+str(i)]
    for frame in [0,10,20,30,40]:
        obj.location.x=math.sin(frame*math.pi/20-i*.65)*.08;obj.keyframe_insert(data_path='location',frame=frame)
scene.frame_start=0;scene.frame_end=40;scene.frame_set(0)
camdata=bpy.data.cameras.new('Nasuko Reinforcements Preview');camera=bpy.data.objects.new(camdata.name,camdata);scene.collection.objects.link(camera)
camera.location=(10,17,10);camera.rotation_euler=(Vector((0,-.6,1.4))-camera.location).to_track_quat('-Z','Y').to_euler();camdata.type='ORTHO';camdata.ortho_scale=14.5;scene.camera=camera
scene.render.resolution_x=1600;scene.render.resolution_y=1000
for pos,energy,size in [((1,-5,9),1800,8),((-7,2,6),1600,7),((6,4,7),1200,6)]:
    data=bpy.data.lights.new('Nasuko Reinforcement Softbox','AREA');data.energy=energy;data.shape='DISK';data.size=size
    light=bpy.data.objects.new(data.name,data);scene.collection.objects.link(light);light.location=pos;light.rotation_euler=(Vector((0,0,1))-light.location).to_track_quat('-Z','Y').to_euler()
scene.render.filepath=str(ROOT/'art/dimensional_reinforcements_preview.png')
bpy.data.libraries.write(str(ROOT/'art/dimensional_reinforcements.blend'),{scene},fake_user=True,compress=True,path_remap='RELATIVE_ALL')
bpy.ops.render.render(write_still=True);bpy.context.window.scene=original_scene
result={'status':'ok','models':summary,'preview':scene.render.filepath}
