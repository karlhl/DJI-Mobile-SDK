// 矢量图层源
var vectorSource = new ol.source.Vector();
// 矢量图层
var vectorLayer = new ol.layer.Vector({
    source: vectorSource,
    style: new ol.style.Style({
        fill: new ol.style.Fill({
            color: 'rgba(255, 255, 255, 0.2)'
        }),
        stroke: new ol.style.Stroke({
            color: '#ffcc33',
            width: 2
        })
    })
});

// 航线样式
var style_waypoint_line = new ol.style.Style ({
    stroke: new ol.style.Stroke({
        color: '#000000',
        width: 1
    })
});

// Google底图
var rasterLayer = new ol.layer.Tile({
    source: new ol.source.XYZ({
        url: 'http://mt{0-2}.google.cn/vt/lyrs=m&hl=zh-CN&gl=cn&x={x}&y={y}&z={z}'

    })
});

// OpenLayer地图对象，包含Google底图图层和用于规划航线的矢量图层
var map = new ol.Map({
    controls: [new ol.control.Zoom()],
    layers: [rasterLayer, vectorLayer],
    target: document.getElementById('map'),
    view: new ol.View({
        center: ol.proj.transform([107.982762, 33.95895],'EPSG:4326', 'EPSG:3857'),
        zoom: 5
    })
});

// 修改对象
var modify = new ol.interaction.Modify({source: vectorSource});
map.addInteraction(modify);

// 绘制对象
var draw;

// 在地图中增加绘制对象，其类型为多边形，用于绘制航线规划区域
function addInteractions() {
    draw = new ol.interaction.Draw({
        source: vectorSource,
        type: 'Polygon'
    });
    map.addInteraction(draw);
}

// 在进入地图时开始绘制
addInteractions();

// 生成航线和航点
function generateWayPoints(){
	// 航线间隔
    const internal = 100; 
	// 航点
    let waypoints = [];
	// 获取航测区
    let polygon = vectorSource.getFeatures()[0].getGeometry();
    // 航测区的四至范围
    let minx = Math.floor(polygon.getExtent()[0]);
    let miny = Math.floor(polygon.getExtent()[1]);
    let maxx = Math.floor(polygon.getExtent()[2]);
    let maxy = Math.floor(polygon.getExtent()[3]);
	// Y轴方向的坐标差
    let ylength = maxy - miny;
	// 计算所需要的航线数量
    let pathwaycount = Math.ceil((ylength - Math.floor(internal / 2)) / internal);
	// 与X轴平行，逐一生成每一条航线
    for(let i = 0; i < pathwaycount; i ++) {
        // 1、新建航线，让航线贯串多边形，X范围从minx到maxx。
        // 航线的Y值
        var y = miny + Math.floor(internal / 2) + internal * i;
        // 航线两端的坐标
        var pathway_coor1 = [minx, y];
        var pathway_coor2 = [maxx, y];
		// 航线对象
        var pathway = [pathway_coor1, pathway_coor2];

        // 2、截取航线，获得航点
        let waypointsInPathway = [];
        // 航测区域线段数量
        let count = polygon.getLinearRing(0).getCoordinates().length - 1; // 最后一个点与第一个点重复。
        // 遍历航测区的各个线段
        for(let j = 0; j < count; j ++) {
            // 生成航测区的线段
            let segment_coor1 = polygon.getLinearRing(0).getCoordinates()[j];
            let segment_coor2 = polygon.getLinearRing(0).getCoordinates()[j + 1];
            let segment = [segment_coor1, segment_coor2];

            // 判断航线是否直接通过线段的端点
            if (segment_coor1[1] == y){
                // 取得该端点相邻的两个点的Y值
                let y1 = polygon.getLinearRing(0).getCoordinates()[j - 1 < 0 ? count - 2 : j - 1][1];
                let y2 = segment_coor2[1];
				// 在同一条直线上，且与X轴平行，略过该点
                if (y1 == y2 && y1 == y)
                    continue;
                if (y1 > y ^ y2 > y) {
                    waypointsInPathway.push(segment_coor1);
                }
            }
			// 在下一条线段中处理该端点
            if (segment_coor2[1] == y)
                continue;

            // 航测区线段与航线有交点，计算该航点
            if (segment_coor1[1] > y ^ segment_coor2[1] > y) {
                // 计算航点
                var intersection_coor = intersectionOfSegments(pathway, segment);
                waypointsInPathway.push(intersection_coor);
            }

        }

        // 按从小到大的顺序排列当前航线的航点
        waypointsInPathway.sort(function(a, b) {
            return a[0] - b[0];
        });
        // 如果航线为偶数行，则倒置航点顺序
        if (i % 2 == 1) {
            waypointsInPathway.reverse();
        }
        waypoints = waypoints.concat(waypointsInPathway);
        waypointsInPathway = [];
    }


    // 此时航点已经完全生成，下面的代码连接航点生成航线，并显示在地图上
    var waypoints_line = new ol.geom.LineString(waypoints, 'XY');
    var feature = new ol.Feature({geometry: waypoints_line});
    feature.setStyle(style_waypoint_line);
    vectorSource.addFeature(feature);

}

// 求两个线段（直线）的交点
function intersectionOfSegments(segment1, segment2) {
    let A1 = segment1[1][1] - segment1[0][1];
    let B1 = segment1[0][0] - segment1[1][0];
    let C1 = segment1[1][0] * segment1[0][1] - segment1[0][0] *segment1[1][1];

    let A2 = segment2[1][1] - segment2[0][1];
    let B2 = segment2[0][0] - segment2[1][0];
    let C2 = segment2[1][0] * segment2[0][1] - segment2[0][0] *segment2[1][1];

    let intersection_x = (C2 * B1 - C1 * B2) / (A1 * B2 - A2 * B1);
    let intersection_y = (C1 * A2 - C2 * A1) / (A1 * B2 - A2 * B1);

    return [intersection_x, intersection_y];
}
