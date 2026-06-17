import fallbackImage from "../../images/ProductImages/perfume_flores.jpg";

const productImageContext = require.context(
  "../../images/ProductImages",
  false,
  /\.(png|jpe?g|jfif|webp)$/i,
);

const normalize = (value) =>
  String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim();

const productImages = productImageContext.keys().reduce((images, key) => {
  const baseName = key.replace("./", "").replace(/\.[^.]+$/, "");
  images.set(normalize(baseName), productImageContext(key));
  return images;
}, new Map());

const getImageByName = (name) => productImages.get(normalize(name)) || fallbackImage;

const productImageRules = [
  { category: "perfumes", tokens: ["eucalipto"], image: "Perfume_Eucalipto" },
  { category: "perfumes", tokens: ["lavanda"], image: "colonia_lavanda" },
  { category: "perfumes", tokens: ["rosa"], image: "perfume_flores_2" },
  { category: "perfumes", tokens: ["flores"], image: "perfume_flores" },
  { category: "bloqueadores", tokens: ["mineral"], image: "bloqueador_1" },
  { category: "bloqueadores", tokens: ["facial"], image: "bloqueador_2" },
  { category: "bloqueadores", tokens: ["corporal"], image: "bloqueador_3" },
  { category: "bloqueadores", tokens: ["infantil"], image: "bloqueador_4" },
  { category: "bloqueadores", tokens: ["aloe"], image: "bloqueador_1" },
  { category: "jabones", tokens: ["mano", "natural"], image: "jabon_natural" },
  { category: "jabones", tokens: ["corporal"], image: "jabon_natural_2" },
  { category: "jabones", tokens: ["liquido"], image: "jabon_liquido" },
  { category: "jabones", tokens: ["artesanal"], image: "jabon_natural_3" },
  { category: "botellas", tokens: ["vidrio"], image: "botella_vidrio" },
  { category: "botellas", tokens: ["plastica"], image: "botella_plastico" },
  { category: "botellas", tokens: ["termica"], image: "botella_metal" },
  { category: "botellas", tokens: ["acero"], image: "botella_metal" },
  { category: "botellas", tokens: ["bambu"], image: "botella_bambu" },
  { category: "botellas", tokens: ["deportiva"], image: "botella_vidrio_2" },
  { category: "aceites herbales", tokens: ["lavanda"], image: "aceite_1" },
  { category: "aceites herbales", tokens: ["romero"], image: "aceite_2" },
  { category: "aceites herbales", tokens: ["manzanilla"], image: "aceite_3" },
  { category: "aceites herbales", tokens: ["relajante"], image: "aceite_4" },
];

const categoryFallbacks = {
  perfumes: "perfume_flores",
  bloqueadores: "bloqueador_1",
  jabones: "jabon_natural",
  botellas: "botella_vidrio",
  "aceites herbales": "aceite_1",
};

const getProductImage = (product) => {
  const productName = normalize(product?.nombre);
  const category = normalize(product?.categoria?.categoria);

  const rule = productImageRules.find((item) => {
    const matchesCategory = !item.category || item.category === category;
    const matchesTokens = item.tokens.every((token) => productName.includes(token));
    return matchesCategory && matchesTokens;
  });

  if (rule) return getImageByName(rule.image);

  const directMatch = Array.from(productImages.entries()).find(([imageName]) =>
    imageName
      .split(" ")
      .filter((token) => Number.isNaN(Number(token)))
      .every((token) => productName.includes(token)),
  );

  if (directMatch) return directMatch[1];

  return getImageByName(categoryFallbacks[category]);
};

export { getProductImage };
