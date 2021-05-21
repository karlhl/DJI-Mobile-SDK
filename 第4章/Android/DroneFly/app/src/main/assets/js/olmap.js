
// 飞机图标对象
var icon = new ol.style.Icon({
    anchor: [0.50, 0.50],
    anchorXUnits: 'fraction',
    anchorYUnits: 'fraction',
    src: 'ic_plane.svg'
});

// 显示飞机图标的点要素符号样式
var iconStyle = new ol.style.Style({
    image: icon
});

// 飞机点要素
var iconFeature = new ol.Feature({
    geometry: mapPointFromlatlon(125.718691,43.694245),
    name: 'DroneLocation'
});

// 为飞机点要素设置样式
iconFeature.setStyle(iconStyle);

// 显示飞机点要素的矢量图层
var vectorLayer = new ol.layer.Vector({
    source: new ol.source.Vector({
        features: [iconFeature]
    })
});

// 显示Google地图的底图图层
var rasterLayer = new ol.layer.Tile({
    source: new ol.source.XYZ({
        url: 'https://mt{0-2}.google.cn/vt/lyrs=m&hl=zh-CN&gl=cn&x={x}&y={y}&z={z}'

    })
});

// 创建地图对象
var map = new ol.Map({
    controls: [new ol.control.Zoom()],
    layers: [rasterLayer, vectorLayer],
    target: document.getElementById('map'),
    interactions: ol.interaction.defaults({altShiftDragRotate:false, pinchRotate:false}),
    view: new ol.View({
        center: ol.proj.transform([107.982762, 33.95895],'EPSG:4326', 'EPSG:3857'),
        zoom: 5
    })
});

// 通过经纬度定义点对象（在墨卡托坐标系统下）
function mapPointFromlatlon(lon, lat){
    var coordinate = wgs84togcj02(lon, lat);
    coordinate = ol.proj.transform(coordinate,'EPSG:4326', 'EPSG:3857');
    return new ol.geom.Point(coordinate);
}

// 改变飞机点要素位置和方向
function changeDroneLocation(lon, lat, heading) {
    icon.setRotation(heading);
    iconFeature.setGeometry(mapPointFromlatlon(lon, lat));
}

var PI = 3.1415926535897932384626;
var a = 6378245.0;
var ee = 0.00669342162296594323;

// 将WGS84坐标系坐标转为GCJ02坐标系坐标
function wgs84togcj02(lon, lat) {
    var dlat = transformlat(lon - 105.0, lat - 35.0);
    var dlon = transformlon(lon - 105.0, lat - 35.0);
    var radlat = lat / 180.0 * PI;
    var magic = Math.sin(radlat);
    magic = 1 - ee * magic * magic;
    var sqrtmagic = Math.sqrt(magic);
    dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);
    dlon = (dlon * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI);
    var mglat = lat + dlat;
    var mglon = lon + dlon;
    return [mglon, mglat]
}

function transformlat(lon, lat) {
    var ret = -100.0 + 2.0 * lon + 3.0 * lat + 0.2 * lat * lat + 0.1 * lon * lat + 0.2 * Math.sqrt(Math.abs(lon));
    ret += (20.0 * Math.sin(6.0 * lon * PI) + 20.0 * Math.sin(2.0 * lon * PI)) * 2.0 / 3.0;
    ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
    ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
    return ret
}

function transformlon(lon, lat) {
    var ret = 300.0 + lon + 2.0 * lat + 0.1 * lon * lon + 0.1 * lon * lat + 0.1 * Math.sqrt(Math.abs(lon));
    ret += (20.0 * Math.sin(6.0 * lon * PI) + 20.0 * Math.sin(2.0 * lon * PI)) * 2.0 / 3.0;
    ret += (20.0 * Math.sin(lon * PI) + 40.0 * Math.sin(lon / 3.0 * PI)) * 2.0 / 3.0;
    ret += (150.0 * Math.sin(lon / 12.0 * PI) + 300.0 * Math.sin(lon / 30.0 * PI)) * 2.0 / 3.0;
    return ret
}
