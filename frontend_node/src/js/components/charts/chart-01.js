import ApexCharts from "apexcharts";

const monthLabels = [
  "Ene",
  "Feb",
  "Mar",
  "Abr",
  "May",
  "Jun",
  "Jul",
  "Ago",
  "Sep",
  "Oct",
  "Nov",
  "Dic",
];

const emptyMonthlySales = () => Array.from({ length: 12 }, () => 0);

const getVentaDate = (venta) => {
  if (venta.fecha) return venta.fecha;

  const detalleConFecha = venta.detalles?.find((detalle) => detalle.fecha);
  return detalleConFecha?.fecha;
};

const loadMonthlySales = async () => {
  const response = await fetch("/api/ventas-completas");
  if (!response.ok) throw new Error("No se pudieron cargar las ventas");

  const ventas = await response.json();
  const monthlySales = emptyMonthlySales();

  ventas.forEach((venta) => {
    const fecha = getVentaDate(venta);
    if (!fecha) return;

    const date = new Date(`${fecha}T00:00:00`);
    const month = date.getMonth();

    if (month >= 0 && month < 12) {
      monthlySales[month] += 1;
    }
  });

  return monthlySales;
};

// ===== chartOne
const chart01 = () => {
  const chartOneOptions = {
    series: [
      {
        name: "Ventas",
        data: emptyMonthlySales(),
      },
    ],
    colors: ["#7fbd19"],
    chart: {
      fontFamily: "Outfit, sans-serif",
      type: "bar",
      height: 320,
      toolbar: {
        show: false,
      },
    },
    plotOptions: {
      bar: {
        horizontal: false,
        columnWidth: "39%",
        borderRadius: 5,
        borderRadiusApplication: "end",
      },
    },
    dataLabels: {
      enabled: false,
    },
    stroke: {
      show: true,
      width: 4,
      colors: ["transparent"],
    },
    xaxis: {
      categories: monthLabels,
      axisBorder: {
        show: false,
      },
      axisTicks: {
        show: false,
      },
    },
    legend: {
      show: true,
      position: "top",
      horizontalAlign: "left",
      fontFamily: "Outfit",

      markers: {
        radius: 99,
      },
    },
    yaxis: {
      title: false,
    },
    grid: {
      yaxis: {
        lines: {
          show: true,
        },
      },
    },
    fill: {
      opacity: 1,
    },

    tooltip: {
      x: {
        show: false,
      },
      y: {
        formatter: function (val) {
          return `${val} ventas`;
        },
      },
    },
  };

  const chartSelector = document.querySelectorAll("#chartOne");

  if (chartSelector.length) {
    const chartFour = new ApexCharts(
      document.querySelector("#chartOne"),
      chartOneOptions,
    );
    chartFour.render();

    loadMonthlySales()
      .then((monthlySales) => {
        chartFour.updateSeries([
          {
            name: "Ventas",
            data: monthlySales,
          },
        ]);
      })
      .catch((error) => {
        console.error("Error cargando ventas mensuales:", error);
      });
  }
};

export default chart01;
